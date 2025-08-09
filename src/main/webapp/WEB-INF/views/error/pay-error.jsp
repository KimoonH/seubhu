<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/views/common/tags.jsp" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <%@include file="/WEB-INF/views/common/common.jsp" %>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>결제 오류</title>
</head>
<body>
<%@include file="/WEB-INF/views/common/nav.jsp" %>
<div class="container-xxl text-center align-content-center" id="wrap">
    <div class="row">
        <h1 class="text-danger">결제 오류</h1>
        <p class="">결제 처리 중 오류가 발생했습니다.</p>
        <p class="text-muted">
            <c:choose>
                <c:when test="${not empty errorMessage}">
                    ${errorMessage}
                </c:when>
                <c:otherwise>
                    알 수 없는 오류가 발생했습니다. 다시 시도해주세요.
                </c:otherwise>
            </c:choose>
        </p>
    </div>
    <div class="row justify-content-center">
        <div class="col">
            <a href="/cart" class="btn btn-primary mt-3 me-2 w-25">장바구니로</a>
            <a href="/home" class="btn btn-secondary mt-3 w-25">홈으로 돌아가기</a>
        </div>
    </div>
</div>
<%@include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>