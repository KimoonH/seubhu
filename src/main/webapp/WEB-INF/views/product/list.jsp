<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/views/common/tags.jsp" %>
<!doctype html>
<html lang="ko">
<head>
    <%@include file="/WEB-INF/views/common/common.jsp" %>
</head>
<body>
<%@include file="/WEB-INF/views/common/nav.jsp" %>

<div class="container-xxl text-center" id="wrap">
    <div class="col m-5">
        <h2>상품 전체 페이지</h2>
    </div>
    <c:choose>
        <c:when test="${topNo == 10}">
            <div class="row row-cols-2 row-cols-lg-6 g-2 g-lg-3">
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=10" >전체보기</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=10&catNo=11" >남성 러닝화</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=10&catNo=12">남성 상의</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=10&catNo=13">남성 하의</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=10&catNo=14">남성 아우터</a>
                </div>
            </div>
        </c:when>
        <c:when test="${topNo == 20}">
            <div class="row row-cols-2 row-cols-lg-6 g-2 g-lg-3">
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=20" >전체보기</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=20&catNo=21" >여성 러닝화</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=20&catNo=22">여성 상의</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=20&catNo=23">여성 하의</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=20&catNo=24">여성 아우터</a>
                </div>
            </div>
        </c:when>
        <c:when test="${topNo == 30}">
            <div class="row row-cols-2 row-cols-lg-5 g-2 g-lg-3">
                <div class="col " >
                <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=30">전체보기</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=30&catNo=31">양말</a>
                </div>
                <div class="col " >
                    <a class="nav-link p-3 border-start border-primary border-4 bg-light" style="border-color: #0064FF;" href="list?topNo=30&catNo=32">모자</a>
                </div>
            </div>
        </c:when>
    </c:choose>
    <div class="row mt-3">
        <div class="col-12">
            <form id="form-search" method="get" action="list">
                <input type="hidden" name="page" />
                <input type="hidden" name="topNo" value="${topNo}">
                <input type="hidden" name="catNo" value="${catNo}">
                <div class="row g-3">
                    <div class="col-2">
                        <select class="form-control" name="rows" onchange="changeRows()">
                            <option value="6" ${empty param.rows or param.rows eq 6? "selected" : ""}>6개씩 보기</option>
                            <option value="12" ${param.rows eq 12? "selected" : ""}>12개씩 보기</option>
                            <option value="18" ${param.rows eq 18? "selected" : ""}>18개씩 보기</option>
                        </select>
                    </div>
                    <div class="col-4 pt-2">
                        <div class="form-check form-check-inline">
                            <input class="form-check-input"
                                   type="radio"
                                   name="sort"
                                   value="date"
                                   onchange="changeSort()"
                                   ${empty param.sort or param.sort eq 'date' ? 'checked' : ''}
                            >
                            <label class="form-check-label" >최신순</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input"
                                   type="radio"
                                   name="sort"
                                   value="name"
                                   onchange="changeSort()"
                                   ${param.sort eq 'name' ? 'checked' : ''}
                            >
                            <label class="form-check-label" >이름순</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input"
                                   type="radio"
                                   name="sort"
                                   value="price"
                                   onchange="changeSort()"
                                   ${param.sort eq 'price' ? 'checked' : ''}
                            >
                            <label class="form-check-label" >가격순</label>
                        </div>
                    </div>
                    <div class="col-2">
                        <select class="form-control" name="opt">
                            <option value="name" ${param.opt eq 'name' ? 'selected' : ''} >상품명</option>
                            <option value="minPrice" ${param.opt eq 'minPrice' ? 'selected' : '' }>최소가격</option>
                            <option value="maxPrice" ${param.opt eq 'maxPrice' ? 'selected' : '' }>최대가격</option>
                        </select>
                    </div>
                    <div class="col-3">
                        <input type="text" class="form-control" name="value" value=${param.value}>
                    </div>
                    <div class="col-1">
                        <button class="btn btn-outline-primary" onclick="searchValue()">검색</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
        <div class="row row-cols-1 row-cols-md-3 g-4 mt-1 mb-5">
            <c:forEach var="prod"  items="${products }">
                <div class="col">
                    <div class="card h-100">
                         <a class="text-decoration-none" href="/product/detail?productNo=${prod.no}&colorNo=${prod.colorNum}">
                            <img src="${prod.imgThum}" class="card-img-top" alt="...">
                        </a>
                        <div class="card-body">
                            <h5 class="card-title">${prod.name}</h5>
                            <div class="text-decoration-none" href="detail">
                                <div>${prod.brand.name}</div>
                                <div>${prod.category.name}</div>
                                <div class="card-text"><fmt:formatNumber value="${prod.price }"/> 원</div>
                            </div>
                        </div>
                        <div class="card-footer bg-transparent border-primary" >${prod.status}</div>
                    </div>
                </div>
            </c:forEach>
        </div>
        <!-- 페이징 처리 -->
            <div class="row mb-3">
				<div class="col-12">
                    <nav>
                        <ul class="pagination justify-content-center">
                            <li class="page-item ${paging.first? 'disabled' : ''}">
                                <a class="page-link"
                                onclick="changePage(${paging.prevPage}, event)"
                                href="list?page=${paging.prevPage}">이전</a>
                            </li>
                            <c:forEach var="num" begin="${paging.beginPage}" end="${paging.endPage}">
                                <li class="page-item ${paging.page eq num ? 'active' : ''}">
                                    <a class="page-link"
                                    onclick="changePage(${num}, event)"
                                    href="list?page=${num}">${num}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item ${paging.last ? 'disabled' : ''}" >
                                <a class="page-link"
                                onclick="changePage(${paging.nextPage}, event)"
                                href="list?page=${paging.nextPage}">다음</a>
                            </li>
                        </ul>
                    </nav>
                </div>
            </div>
    <script type="text/javascript">
        const form = document.querySelector("#form-search")
        const pageInput = document.querySelector("input[name=page]");

        // 페이지 번호 링크를 클릭했을 때 변화
        function changePage(page, event) {
            event.preventDefault();
            // 페이지 번호 링크를 클릭했다면 해당 페이징 요청
            pageInput.value = page;

            form.submit();
        }

        // 정렬방식이 변결될 때
        function changeSort() {
            pageInput.value = 1;
            form.submit();
        }

        // 검색어를 입력하고 검색버튼을 클릭 했을 때
        function searchValue() {
            pageInput.value = 1;
            form.submit();

        }

        // 한 화면에 표기할 행의 갯수가 변경될 때
        function changeRows() {
            pageInput.value = 1;
            form.submit();
        }
    </script>
<%@include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>