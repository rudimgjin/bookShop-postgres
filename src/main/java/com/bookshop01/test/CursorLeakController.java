package com.bookshop01.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller("cursorLeakController")
@RequestMapping("/test/cursorLeak")
public class CursorLeakController {

    @Autowired
    private DataSource dataSource;

    // 💡 단 하나의 Connection만 유지하여 단일 DB 세션 내 커서 폭주를 유발합니다.
    private static Connection singleLeakConn = null;
    // 💡 누수된 Statement 객체를 메모리에 보관합니다.
    private static final List<Statement> leakedStatements = Collections.synchronizedList(new ArrayList<Statement>());

    private static int requestCount = 0;
    private static String errorMessage = null;
    // private static boolean permanentError = false; // 💡 더 이상 사용하지 않습니다.

    // 💡 DB 접속 정보 (순수 JDBC 연결용 - DB 전체 커서 조회용)
    private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static final String DB_URL    = "jdbc:oracle:thin:@jadelab.iptime.org:9813:orcl";
    private static final String DB_USER   = "SCOTT";
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

        // ORA-01000이 발생했었다면 메시지를 미리 설정합니다.
        if (errorMessage != null && errorMessage.contains("ORA-01000")) {
            currentMessage = "🚨 ORA-01000 에러 고착 상태 (재시도 중)";
            mav.addObject("messageType", "error");
            mav.addObject("currentError", errorMessage);
        } else {
            errorMessage = null; // 정상적인 누적 중일 때 에러 메시지 초기화
        }

        try {
            // 1. Connection 확보: Pool에서 Connection을 처음 한 번만 빌려옵니다.
            if (singleLeakConn == null || singleLeakConn.isClosed()) {
                singleLeakConn = dataSource.getConnection();
                currentMessage = "단일 Connection 확보 성공. 커서 누적 시작.";
            }

            // 2. 단일 Connection 내에서 하나의 커서만 엽니다.
            // 🔴 이 시점에서 ORA-01000이 발생해야 합니다.
            Statement stmt = singleLeakConn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM T_SHOPPING_MEMBER");

            // 커서 누수 유발 (close() 호출 생략)

            currentStatements.add(stmt);

            // 성공적으로 누적된 Statement를 영구 리스트에 추가
            leakedStatements.addAll(currentStatements);
            currentMessage = "단일 Connection 내에서 커서 1개 누적 성공. (총 누적 커서: " + (leakedStatements.size()) + "개)";
            mav.addObject("messageType", "success");

        } catch (SQLException e) {
            errorOccurred = true;
            errorMessage = e.getMessage();

            if (e.getErrorCode() == 1000) { // ORA-01000: maximum open cursors exceeded
                currentMessage = "🚨 ORA-01000 에러 발생! 커서 한계를 초과했습니다. (지속적 발생 시도)";
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
            // 🔴 ORA-01000이 발생해도 Connection을 Pool에 반납하거나 자원을 정리하지 않습니다. (고착 유지)
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
        result.put("currentError", errorMessage); // 마지막 에러 메시지 반환

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

            // DB 전체의 현재 열린 커서 개수 총합 조회 쿼리
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
            // 순수 JDBC 연결 자원은 반드시 닫아야 합니다!
            try {
                if (statusRs != null) statusRs.close();
                if (statusStmt != null) statusStmt.close();
                if (statusConn != null) statusConn.close();
            } catch (SQLException ignore) {}
        }
        return currentTotalCursors;
    }
}