package com.bookshop01.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody; // 💡 추가
import org.springframework.web.servlet.ModelAndView;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager; // 💡 추가
import java.util.ArrayList;
import java.util.List;
import java.util.Collections; // 💡 추가
import java.util.HashMap; // 💡 추가
import java.util.Map; // 💡 추가
@Controller("cursorLeakController")
@RequestMapping("/test/cursorLeak")
public class CursorLeakController {

    @Autowired
    private DataSource dataSource;

    // 💡 단 하나의 Connection만 유지하여 단일 DB 세션 내 커서 폭주를 유발합니다.
    private static Connection singleLeakConn = null;
    // 💡 누수된 Statement 객체를 메모리에 보관합니다. (Java 6 호환)
    private static final List<Statement> leakedStatements = Collections.synchronizedList(new ArrayList<Statement>());

    private static int requestCount = 0;
    private static String errorMessage = null;

    // 💡 DB 접속 정보 (순수 JDBC 연결용 - DB 전체 커서 조회용)
    private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static final String DB_URL    = "jdbc:oracle:thin:@jadelab.iptime.org:9813:orcl";
    private static final String DB_USER   = "SCOTT"; // 조회 권한이 있는 DB 사용자
    private static final String DB_PASS   = "12341234";

    /**
     * [1] ORA-01000 에러 유발 및 초기 페이지 로드 처리
     */
    @RequestMapping(value = "/leakConnection.do", method = RequestMethod.GET)
    public ModelAndView leakConnection() {
        ModelAndView mav = new ModelAndView();
        String currentMessage = null;
        requestCount++;

        // 현재 요청에서 발생한 Statement 임시 저장소
        List<Statement> currentStatements = new ArrayList<Statement>();
        boolean errorOccurred = false;

        if (errorMessage != null && errorMessage.contains("ORA-01000")) {
            currentMessage = "⚠️ ORA-01000 에러가 이미 발생했습니다. DB/WAS 재시작이 필요합니다.";
            mav.addObject("messageType", "error");
            mav.addObject("currentError", errorMessage);
        } else {
            try {
                // 1. Connection 확보: Pool에서 Connection을 처음 한 번만 빌려옵니다.
                if (singleLeakConn == null || singleLeakConn.isClosed()) {
                    singleLeakConn = dataSource.getConnection();
                    currentMessage = "단일 Connection 확보 성공. 커서 누적 시작.";
                }

                // 2. 단일 Connection 내에서 30개의 커서를 반복적으로 엽니다. (OPEN_CURSORS 한계 도달 유도)
                for(int i = 0; i < 30; i++) {
                    Statement stmt = singleLeakConn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM T_SHOPPING_MEMBER WHERE ROWNUM < 2");

                    // 🔴 커서 누수 유발 지점: ResultSet과 Statement는 열었지만 닫지 않습니다.
                    // rs.close();
                    // stmt.close();

                    currentStatements.add(stmt);
                }

                // 🔴 Connection 반납 누락: Connection은 계속 열린 상태로 유지됩니다.
                // singleLeakConn.close();

                // 성공적으로 누적된 Statement를 영구 리스트에 추가
                leakedStatements.addAll(currentStatements);
                currentMessage = "단일 Connection 내에서 커서 " + (currentStatements.size()) + "개 누적 성공. (총 누적 커서: " + (leakedStatements.size()) + "개)";
                mav.addObject("messageType", "success");

            } catch (SQLException e) {
                errorOccurred = true;
                errorMessage = e.getMessage();

                if (e.getErrorCode() == 1000) { // ORA-01000: maximum open cursors exceeded
                    currentMessage = "🚨 ORA-01000 에러 발생! 커서 한계를 초과했습니다.";
                } else {
                    currentMessage = "DB 작업 중 오류 발생: " + e.getMessage();
                }

                mav.addObject("messageType", "error");
                mav.addObject("currentError", errorMessage);
            } catch (Exception e) {
                errorOccurred = true;
                currentMessage = "알 수 없는 오류 발생: " + e.getMessage();
                mav.addObject("messageType", "error");
            } finally {
                // 3. ORA-01000 에러 발생 시, 테스트 환경 복구를 위해 누수된 자원을 강제로 정리합니다.
                if (errorOccurred && singleLeakConn != null) {
                    try {
                        // 🔴 예외 발생 시에만 강제 정리:
                        // Connection Pool에 Connection을 반납하고, Pool이 해당 Connection의 커서를 정리하도록 위임합니다.
                        // (테스트 환경 복구를 위해 반드시 필요)
                        singleLeakConn.close();
                        singleLeakConn = null;
                        leakedStatements.clear();
                    } catch (SQLException ignore) {}
                }
            }
        }

        // 4. DB 전체 커서 개수 조회 (순수 JDBC 연결 사용)
        int currentTotalCursors = getCurrentTotalCursors();

        mav.addObject("testMessage", currentMessage);
        mav.addObject("requestCount", requestCount);
        mav.addObject("totalCursors", leakedStatements.size());
        mav.addObject("currentTotalCursors", currentTotalCursors);

        mav.setViewName("/test/dbcpLeakTest");

        return mav;
    }

    /**
     * [2] AJAX 요청 처리: 커서 상태를 JSON 형태로 반환
     */
    @RequestMapping(value = "/getCursorsStatus", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getCursorsStatus() {

        int currentTotalCursors = getCurrentTotalCursors();

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("currentTotalCursors", currentTotalCursors);
        result.put("requestCount", requestCount);
        result.put("totalCursors", leakedStatements.size());
        result.put("currentError", errorMessage);

        return result;
    }

    /**
     * [Helper Method] DB 전체 커서 개수를 조회합니다.
     */
    private int getCurrentTotalCursors() {
        Connection statusConn = null;
        Statement statusStmt = null;
        ResultSet statusRs = null;
        int currentTotalCursors = -1;

        try {
            Class.forName(DB_DRIVER);
            statusConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            statusStmt = statusConn.createStatement();

            // DB 전체의 현재 열린 커서 개수 총합 조회 쿼리 (특정 USERNAME 필터링 없음)
            String cursorQuery =
                    "SELECT SUM(A.VALUE) FROM V$SESSTAT A, V$STATNAME B " +
                            "WHERE A.STATISTIC# = B.STATISTIC# " +
                            "AND B.NAME = 'opened cursors current'";

            statusRs = statusStmt.executeQuery(cursorQuery);

            if (statusRs.next()) {
                currentTotalCursors = statusRs.getInt(1);
            }

        } catch (Exception statusEx) {
            System.err.println("커서 상태 확인 중 오류 발생: " + statusEx.getMessage());
        } finally {
            // 🔴 커서 상태 조회용 연결은 Pool 누수 테스트와 무관하게 반드시 닫아야 합니다. (물리적 연결 종료)
            try {
                if (statusRs != null) statusRs.close();
                if (statusStmt != null) statusStmt.close();
                if (statusConn != null) statusConn.close();
            } catch (SQLException ignore) {}
        }
        return currentTotalCursors;
    }
}