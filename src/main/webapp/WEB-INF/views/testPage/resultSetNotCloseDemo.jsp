<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.sql.*, java.util.*" %>
<%-- 💡 jQuery와 Chart.js 라이브러리 로드 --%>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@2.9.4/dist/Chart.min.js"></script>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>ORA-01000 유발 및 모니터링 테스트</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #cc0000; }
        .instructions { border: 1px solid #ccc; padding: 15px; margin-top: 20px; background-color: #f9f9f9; }
        .action-link { display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 15px; }
        .warning { color: orange; font-weight: bold; }
        .message-box { margin-top: 20px; padding: 10px; border: 1px solid #ddd; }
        .success { color: green; }
        .error { color: red; font-weight: bold; }
        #status-display { background-color: #ffeaea; border: 2px solid #cc0000; padding: 10px; margin-top: 20px; }
        #chart-container { margin-top: 30px; border: 1px solid #ccc; padding: 10px; }
    </style>
</head>
<body>

<h1>ORA-01000 유발 및 실시간 커서 모니터링</h1>

<%
    // 초기 로딩 시 Controller에서 전달받는 값
    String message = (String) request.getAttribute("testMessage");
    String messageType = (String) request.getAttribute("messageType");
    Integer requestCount = (Integer) request.getAttribute("requestCount");
    String currentError = (String) request.getAttribute("currentError");
    Integer totalCursors = (Integer) request.getAttribute("totalCursors");
    Integer currentTotalCursors = (Integer) request.getAttribute("currentTotalCursors"); // DB 전체 커서 개수
%>

<div class="instructions">
    <h3>테스트 실행 및 응답</h3>
    <p>아래 링크를 반복적으로 클릭하여 단일 DB 세션에 커서를 누적시킵니다.</p>

    <a href="<c:url value='/test/cursorLeak/leakConnection.do'/>" class="action-link">
        커서 누수 유발 및 ORA-01000 시도 (반복 클릭)
    </a>
</div>

<% if (message != null) {
    String cssClass = (messageType != null && messageType.equals("error")) ? "error" : "success";
%>
<div class="message-box">
    <h3>Controller 응답 결과 (최종 요청 시)</h3>
    <p class="<%= cssClass %>"><strong><%= message %></strong></p>
</div>
<% } %>

<hr>
<%-- 💡 동적 업데이트될 상태 영역 --%>
<div id="status-display">
    <h3>테스트 상태 (1초마다 자동 업데이트)</h3>
    <p>
        누적 요청 횟수: <span id="request-count" style="font-size: 1.5em; color: blue; font-weight: bold;"><%= requestCount != null ? requestCount : "0" %> 회</span>
    </p>
    <p>
        Controller 인지 총 누적 커서 (Statement 개수):
        <span id="controller-cursors" style="font-size: 1.5em; color: #0077b6; font-weight: bold;"><%= totalCursors != null ? totalCursors : "0" %> 개</span>
    </p>
    <p>
        DB 전체 열린 커서 수:
        <span id="db-total-cursors" style="font-size: 1.5em; color: #cc0000; font-weight: bold;"><%= currentTotalCursors != null ? currentTotalCursors : "측정 불가" %> 개</span>
    </p>

    <div id="error-message-box">
        <% if (currentError != null && currentError.contains("ORA-01000")) { %>
        <p class="error">
            ORA-01000 에러 발생!<br>
            에러 메시지: <%= currentError %>
        </p>
        <% } %>
    </div>
</div>
<hr>

<%-- 💡 그래프 영역 --%>
<div id="chart-container">
    <h2>📉 DB 전체 커서 개수 변화 추이 (실시간)</h2>
    <div style="width: 800px; margin: auto;">
        <canvas id="cursorChart"></canvas>
    </div>
</div>
<hr>

<h3>관찰 방법</h3>
<p>
    1. 그래프: 링크를 클릭하면 커서 개수가 급증하며 그래프의 선이 수직으로 올라가는지 확인하세요.<br>
    2. 에러: ORA-01000 발생 시, 그래프가 멈추고 에러 메시지가 표시됩니다.
</p>

<script type="text/javascript">
    var monitorInterval = 1000; // 3초 간격으로 폴링
    var timeIndex = 0; // 시계열 레이블을 위한 인덱스
    var cursorChart; // Chart.js 인스턴스를 저장할 변수

    // 데이터 구조 초기화
    var chartData = {
        labels: [], // 시간/요청 인덱스
        datasets: [{
            label: 'DB 전체 열린 커서 수',
            data: [], // 커서 개수 값
            backgroundColor: 'rgba(255, 99, 132, 0.2)',
            borderColor: 'rgba(255, 99, 132, 1)',
            borderWidth: 2,
            fill: false,
            lineTension: 0.1 // 부드러운 선형
        }]
    };

    // 1. Chart.js 초기화 함수
    function initChart() {
        var ctx = document.getElementById('cursorChart').getContext('2d');
        cursorChart = new Chart(ctx, {
            type: 'line',
            data: chartData,
            options: {
                responsive: true,
                scales: {
                    yAxes: [{
                        ticks: {
                            beginAtZero: true,
                            // y축은 데이터에 맞게 동적으로 조정됨
                        },
                        scaleLabel: {
                            display: true,
                            labelString: '커서 개수'
                        }
                    }],
                    xAxes: [{
                        scaleLabel: {
                            display: true,
                            labelString: '시간 경과 (1초 간격)'
                        }
                    }]
                }
            }
        });
    }

    // 2. 상태 모니터링 및 그래프 갱신 함수 (AJAX 폴링)
    function monitorStatus() {
        $.ajax({
            url: '<c:url value='/test/cursorLeak/getCursorsStatus'/>',
            type: 'GET',
            dataType: 'json',
            success: function(data) {
                // DOM 요소 업데이트
                $('#request-count').text(data.requestCount + ' 회');
                $('#controller-cursors').text(data.totalCursors + ' 개');

                var currentCursors = data.currentTotalCursors;
                $('#db-total-cursors').text(currentCursors + ' 개');

                // 그래프 데이터 갱신
                if (cursorChart) {
                    timeIndex++;

                    // X축 (레이블) 추가
                    cursorChart.data.labels.push(timeIndex);

                    // Y축 (데이터) 추가
                    cursorChart.data.datasets[0].data.push(currentCursors);

                    // 그래프 갱신
                    cursorChart.update();
                }

                // ORA-01000 에러 메시지 처리
                var errorBox = $('#error-message-box');
                var currentError = data.currentError;

                if (currentError && currentError.includes('ORA-01000')) {
                    errorBox.html(
                        '<p class="error">' +
                        ' ORA-01000 에러 발생!<br>' +
                        '에러 메시지: ' + currentError +
                        '</p>'
                    );
                } else if (errorBox.text().trim() !== '') {
                    errorBox.empty();
                }
            },
            error: function(xhr, status, error) {
                $('#db-total-cursors').text('AJAX 오류: ' + status);
            }
        });
    }

    // 3. 페이지 로드 시 초기화
    $(document).ready(function() {
        initChart();

        // 3초 간격으로 모니터링 시작
        setInterval(monitorStatus, monitorInterval);

        // 초기 로딩 시에도 즉시 한번 실행하여 첫 번째 데이터 포인트를 찍음
        monitorStatus();
    });
</script>

</body>
</html>