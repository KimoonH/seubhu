<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/views/common/tags.jsp" %>
<!doctype html>
<html lang="ko">
<head>
    <%@include file="/WEB-INF/views/common/common.jsp" %>
    <style>
        .star-rating {
            font-size: 2rem;
            cursor: pointer;
        }
        .star-rating .star {
            color: #ddd;
        }
        .star-rating .star.selected {
            color: gold;
        }
    </style>
</head>
<body>
<%@include file="/WEB-INF/views/common/nav.jsp" %>
<div class="container-xxl text-center" id="wrap">

    <h2>상품 상세 페이지</h2>

    <div class="container" style="margin-top: 100px;">
        <div class="row mb-3">
            <%--상품의 사진을 화면에 표시한다.--%>
            <div class="col-6">
                <div class="mb-3 box-big-img">
                    <img src="${prodImagesDto.images.get(0).url}" width="100%" id="big-img"/>
                </div>
                <div class="row">
                    <c:forEach var="imgs" items="${prodImagesDto.images}">
                        <div class="col-2 box-small-img">
                            <img class="img-fluid" src="${imgs.url}" data-big-img-path="${imgs.url}"/>
                        </div>
                    </c:forEach>
                </div>
            </div>
            <div class="col-6">
                <div class="row mb-3">
                    <div class="col-12">
                        <div class="card">
                            <div class="card-header">
                                <h4>상품 상세 정보</h4>
                            </div>
                            <div class="card-body">
                                <table class="table">
                                    <colgroup>
                                        <col width="15%" />
                                        <col width="35%" />
                                        <col width="15%" />
                                        <col width="35%" />
                                    </colgroup>
                                    <tr>
                                        <th>상품 이름</th>
                                        <td colspan="3">${prodDetailDto.productName}</td>
                                    </tr>
                                    <tr>
                                        <th>상품 가격</th>
                                        <td colspan="3"><fmt:formatNumber value="${prodDetailDto.price }"/> 원</td>
                                    </tr>
                                    <tr>
                                        <th>브랜드명</th>
                                        <td>${prodDetailDto.brand.name}</td>
                                        <th>카테고리</th>
                                        <td>${prodDetailDto.category.name}</td>
                                    </tr>
                                    <tr>
                                        <th>평점</th>
                                        <td>${prodDetailDto.rating}</td>
                                        <th>조회수</th>
                                        <td>${prodDetailDto.cnt}</td>
                                    </tr>
                                    <tr>
                                        <th>상품 설명</th>
                                        <td colspan="3">${prodDetailDto.content}</td>
                                    </tr>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-12">
                        <div class="card">
                            <div class="card-header">
                                <h4>상품 옵션 선택</h4>
                            </div>
                            <div>색상을 선택하세요:</div>
                            <div class="card-body">
                                <div class="mb-4">
                                    <c:forEach var="p" items="${colorProdImgDto}">
                                            <c:forEach var="im" items="${p.images}">
                                                <a href="/product/detail?productNo=${p.product.no}&colorNo=${p.colorNo}"><img src="${im.url}" width=15%/></a>
                                            </c:forEach>
                                    </c:forEach>
                                </div>
                                    <div class="mb-4">
                                        <div class="mb-4">
                                            <label class="form-label d-block">사이즈를 선택하세요:</label>
                                            <div class="row row-cols-5 g-3">
                                                <!-- 사이즈 버튼 -->
                                                <c:forEach var="size" items="${sizeAmountDto.sizes }" varStatus="loop">
                                                    <div class="col">
                                                        <input type="radio" class="btn-check" name="size" id="size${size.size}" value="${size.size}" required
                                                        onchange="fn(this)"
                                                        data-name="${prodDetailDto.productName}"
                                                        data-size="${size.size}"
                                                        data-size-no="${size.no}"
                                                        data-color="${sizeAmountDto.colorName}"
                                                        data-color-no="${sizeAmountDto.colorNo}"
                                                        data-no="${prodDetailDto.productNo}"
                                                        >
                                                        <label class="${size.amount == 0 ? "btn btn-outline-danger fixed-size w-100 d-flex align-items-center justify-content-between disabled": "btn btn-outline-secondary fixed-size w-100 d-flex align-items-center justify-content-between"}" for="size${size.size}">
                                                            <span class="ms-2">${size.size}</span>
                                                            <span class="badge bg-secondary">재고:${size.amount}</span>
                                                        </label>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    </div>
                                    <hr class="bg-primary border border-1">
                                    <!--
                                        선택한 상품과 수량
                                    -->
                                    <form id="form-cart" method="post">
                                        <div id="cart" class="d-flex  p-2 border row">

                                        </div>
                                        <hr class="bg-primary border border-1">

                                        <div class="text-end mb-3">
                                            총액: <strong id="total-price">0</strong> 원 (<small id="total-stock">0</small> 개)
                                        </div>
                                        <div class="text-end mb-3">
                                            <button class="btn btn-outline-secondary" type="button" id="cart-add">장바구니 추가</button>
                                            <button class="btn btn-outline-secondary" type="button" id="wish-add" >위시리스트 추가</button>
                                        </div>
                                    </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!--
        리뷰
     -->
    <hr class="bg-primary border border-1">
    <div class="comment-form mb-4">
        <h5 style="text-align: start; font-weight: bold;">리뷰 작성</h5>
        <form id="form-review"  method="post" action="addProdReview" enctype="multipart/form-data">
            <input type="hidden" name="productNo" value="${prodDetailDto.productNo}">
            <input type="hidden" name="userNo" value="${user.no}">
            <input type="hidden" name="colorNo" value="${prodImagesDto.color.no}">
            <input type="hidden" name="prodName" value="${prodDetailDto.productName}"/>
            <input type="hidden" name="colorName" value="${prodImagesDto.color.name}">
            <input type="hidden" name="userNickname" value="${user.nickname}"/>

            <!-- 별점과 댓글 입력 -->
            <div class="row mb-3">
                <!-- 별점 -->
                <div class="col-2" style="text-align: left;">
                    <label class="form-label" style="font-weight: bold;">별점</label>
                    <div class="star-rating" id="commentStarRating" style="font-size: 1.5rem;">
                        <span class="star" data-value="1">&#9733;</span>
                        <span class="star" data-value="2">&#9733;</span>
                        <span class="star" data-value="3">&#9733;</span>
                        <span class="star" data-value="4">&#9733;</span>
                        <span class="star" data-value="5">&#9733;</span>
                    </div>
                    <input type="hidden" name="rating" id="commentRating" value="0">
                </div>

                <!-- 댓글 입력 -->
                <div class="col-10">
                    <input type="text" name="title" class="form-control form-control-lg mb-2" placeholder="리뷰 제목을 작성하세요"
                           style="border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); padding-left: 15px; font-size: 1rem; color: #333;">
                    <textarea name="content" id="commentContent" class="form-control" rows="3" placeholder="댓글을 작성하세요." style="resize: none;"></textarea>
                </div>

                <!--첨부 파일-->
                <div class="col-2"></div>
                <div class="col-10 mt-2">
                    <input type="file" name="reviewFiles" id="reviewFile" class="form-control" multiple>
                    <small class="form-text text-muted">이미지 파일을 선택해주세요 (여러 개 선택 가능).</small>
                </div>
            </div>

            <!-- 버튼 영역 -->
            <div class="row">
                <div class="col text-end">
                    <button type="submit" class="btn btn-success" >등록</button>
                </div>
            </div>
        </form>
    </div>
    <hr class="bg-primary border border-1">

    <c:choose>
        <c:when test="${empty prodReviews}">
            <div class="row comments rounded py-3" style="background-color: #f2f2f2">
                <div class="text-center py-4">
                    <p style="font-size: 1.2rem; color: #555">아직 등록된 리뷰가 없습니다. 첫 번째 리뷰를 작성해보세요!</p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <c:forEach var="review" items="${prodReviews}">
                <div class="mb-3"></div>
                <div id="wrapper-reviews" class="row comments rounded py-3" style="background-color: #f2f2f2" data-reviewno="${review.reviewNo}">
                    <div class="comment">
                        <div class="row align-items-center">
                            <div class="col-3" style="text-align: start;">
                                <img src="https://github.com/mdo.png" alt="프로필 이미지" style="width: 50px; height: 50px;"
                                     class="rounded-circle mb-2">

                                <strong>${review.userNickname}</strong><br/>

                                <span style="font-size: 0.9rem; color: #555;">
                                    <fmt:formatDate value="${review.reviewDate}" pattern="yyyy-MM-dd HH:mm" />
                                </span><br/>
                                <span style="font-size: 0.9rem; color: #555;">${review.prodName} [${review.colorName}]</span>

                                <div class="star-rating" style="font-size: 1.2rem; color: gold;">
                                    <c:forEach begin="1" end="${review.rating}" var="star">
                                        &#9733;
                                    </c:forEach>
                                    <c:forEach begin="1" end="${5 - review.rating}" var="emptyStar">
                                        &#9734;
                                    </c:forEach>
                                </div>
                            </div>

                            <div class="col">
                                <div class="mt-2">
                                    <c:forEach var="img" items="${review.prodReviewImgs}">
                                        <img src="https://2404-bucket-team-1.s3.ap-northeast-2.amazonaws.com/resources/images/product-review/${img.imgName}" alt="리뷰 이미지" class="img-fluid" style="max-height: 100px; object-fit: cover;">
                                    </c:forEach>
                                </div>
                                <p style="margin: 3px;">${review.reviewTitle} : ${review.reviewContent}</p>
                            </div>

                            <div class="col-2 text-end">
                            <c:if test="${review.userNo == user.no}">
                                <button type="button" class="btn btn-warning btn-sm" id="editReviewBtn" onclick="openEditModal(${review.reviewNo})">수정</button>
                                <button type="button" class="btn btn-danger btn-sm" onclick="deleteReview(${review.reviewNo})">삭제</button>
                            </c:if>
                            <c:if test="${review.userNo != user.no}">
                                    <button type="button" class="btn btn-warning btn-sm" id="editReviewBtn" onclick="openEditModal(${review.reviewNo})" disabled>수정</button>
                                <button type="button" class="btn btn-danger btn-sm" disabled>삭제</button>
                            </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

<!-- 수정 모달 창 -->
<div class="modal fade" id="form-modal" tabindex="-1" aria-labelledby="editReviewModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="editReviewModalLabel">리뷰 수정</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <!-- 수정 폼 -->
                <form id="editReviewForm">
                    <input type="hidden" name="userNo" value="${user.no}">
                    <input type="hidden" id="reviewNo" name="no">

                    <!-- 리뷰 제목 -->
                    <div class="mb-3">
                        <label for="reviewTitle" class="form-label">제목</label>
                        <input type="text" id="reviewTitle" name="title" class="form-control">
                    </div>

                    <!-- 리뷰 내용 -->
                    <div class="mb-3">
                        <label for="reviewContent" class="form-label">내용</label>
                        <textarea id="reviewContent" name="content" rows="4" class="form-control"></textarea>
                    </div>

                    <!-- 별점 -->
                    <div class="col-2" style="text-align: left;">
                        <label class="form-label" style="font-weight: bold;">별점</label>
                        <div class="star-rating" id="commentStarRating-edit" style="font-size: 1.0rem;">
                            <span class="star" data-value="1">&#9733;</span>
                            <span class="star" data-value="2">&#9733;</span>
                            <span class="star" data-value="3">&#9733;</span>
                            <span class="star" data-value="4">&#9733;</span>
                            <span class="star" data-value="5">&#9733;</span>
                        </div>
                        <input type="hidden" name="rating" id="commentRating-edit" value="0">
                    </div>

                    <!-- 이미지 업로드 -->
                    <div class="mb-3">
                        <label for="upfiles" class="form-label">이미지 업로드</label>
                        <input type="file" id="upfiles" name="upfiles" class="form-control" multiple>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" onclick="submitEditReview()">저장</button>
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
    function fn(el) {
        let productNo = el.getAttribute("data-no"); // 상품 번호
        let size = el.getAttribute("data-size"); // 상품 사이즈
        let sizeNo = el.getAttribute("data-size-no"); // 상품 사이즈 번호
        let name = el.getAttribute("data-name"); // 상품명
        let color = el.getAttribute("data-color"); // 상품 색상명
        let colorNum = el.getAttribute("data-color-no"); // 색상 번호

        // 중복되어 있는 상품
        let box = document.querySelector("#item-" + sizeNo);
        if (box) {
            let stockInput = box.querySelector(`#item-\${sizeNo} input[name=stock]`);
            stockInput.value = parseInt(stockInput.value) + 1;
        } else {

            let content = `
                     <div id="item-\${sizeNo}">
                          <input type="hidden" name="prodNo" value="\${prodNo}"/>
                          <input type="hidden" name="size" value="\${size}"/>
                          <input type="hidden" name="sizeNo" value="\${sizeNo}"/>
                          <input type="hidden" name="colorNo" value="\${colorNum}"/>
                         <span><small>\${name} </small></span>
                         <p><small>- \${color} / \${size}</small></p>
                         <input type="button" value=" - " name="minus" data-no="\${sizeNo}">
                         <input type="text" name="stock" value="1" id="stock-\${sizeNo}" size="3" max="" style="width: 3rem; text-align: center">
                         <input type="button" value=" + " name="plus" data-no="\${sizeNo}">
                         <div class="text-end">
                            <small><strong id="price-\${sizeNo}"><fmt:formatNumber value="${prodDetailDto.price }"/></strong>원</small>
                            <button type="button" class="btn btn-lg delete-button" data-target-id="#item-\${sizeNo}"><i class="bi bi-x"></i></button>
                         </div>
                         <hr class="bg-primary border border-1">
                     </div>

            `

            $("#cart").append(content);

        }
        updatePrice(sizeNo);
        updateTotals();
    }

    // 삭제하는 기능
    $("#cart").on('click', '.delete-button', function() {
        let id = $(this).data("target-id");
        $(id).remove();
        updateTotals();
    })

    $("#cart").on('click', 'input[name=minus]', function(){
        let no = $(this).attr("data-no");
        // 수량
        let amountInput = document.querySelector('#stock-' + no);
        let currentValue = parseInt(amountInput.value);

        if(currentValue > 1) {
            amountInput.value = currentValue - 1;
            updatePrice(no)
            updateTotals();
        }
    })

    $("#cart").on('click', 'input[name=plus]', function(){
        let no = $(this).attr("data-no");
        let amountInput = document.querySelector('#stock-' + no);
        let currentValue = parseInt(amountInput.value);
        amountInput.value = currentValue + 1;
        updatePrice(no);

        updateTotals();

    })

    // 금액 업데이트 함수
    const updatePrice = (no) => {
        // 수량
        let amountInput = document.querySelector('#stock-' + no);
        // 단가
        let eachPrice = ${prodDetailDto.price};
        // 총액
        let totalPrice = document.getElementById('price-' + no);
        let quantity = parseInt(amountInput.value) || 1;
        let total = eachPrice * quantity;

        totalPrice.textContent = total.toLocaleString();
    }

    // 총금액과 총수량을 계산하는 기능
    const updateTotals = () => {
        // 총 금액 초기화
        let totalPrice = 0;
        // 총 수량 초기화
        let totalStock = 0;

        // 누적 총금액
        let x = $("#cart strong[id^=price]").each(function() {
            let price = $(this).text().replaceAll(/,/g, "");
            totalPrice += parseInt(price)

        });

        // 누적 총수량
        let y = $("#cart input[id^=stock]").each(function() {
            let stock = $(this).val();
            totalStock += parseInt(stock);
        });

        // 출력
        $("#total-price").text(totalPrice.toLocaleString());
        $("#total-stock").text(totalStock.toLocaleString());
    }

    // 이미지 클릭시 화면 대표 이미지 변경
    $(".box-small-img img").click(function () {
        let bigImgPath = $(this).data("big-img-path");
        console.log(bigImgPath);
        $("#big-img").attr("src", bigImgPath);
    });

    // 장바구니에 전달
    $("#cart-add").click(function () {
        alert("장바구니에 담겼습니다.");
        $("#form-cart").attr("action", "/mypage/cart");
        $("#form-cart").trigger("submit");
    });

    // 위시리스트에 전달
    $("#wish-add").click(function () {
        alert("위시리스트에 담겼습니다.");
        $("#form-cart").attr("action", "/mypage/wish");
        $("#form-cart").trigger("submit");
    });

    $(document).ready(function () {
        // 별점 클릭 시
        $("#commentStarRating .star").click(function () {
            var ratingValue = $(this).data("value"); // 클릭한 별의 값 가져오기

            // 별점 값 저장
            $("#commentRating").val(ratingValue);

            // 별점 하이라이트
            $("#commentStarRating .star").each(function () {
                if ($(this).data("value") <= ratingValue) {
                    $(this).css("color", "gold"); // 선택한 별과 이전 별은 금색
                } else {
                    $(this).css("color", "#ddd"); // 나머지는 회색
                }
            });
        });

        // 초기 별점 하이라이트 설정
        $("#commentStarRating .star").each(function () {
            var ratingValue = $("#commentRating").val();
            if ($(this).data("value") <= ratingValue) {
                $(this).css("color", "gold");
            } else {
                $(this).css("color", "#ddd");
            }
        });
    });


    // 별점 요소와 hidden input을 가져옵니다.
    let stars = document.querySelectorAll('.star-rating .star');
    let ratingInput = document.querySelector('#commentRating');

    // 각 별점을 클릭했을 때 이벤트 처리
    stars.forEach(star => {
        star.addEventListener('click', function () {
            // 클릭한 별점의 값 가져오기
            const selectedRating = this.getAttribute('data-value');

            // hidden input에 값 설정
            ratingInput.value = selectedRating;

            // 별점 스타일 업데이트 (선택된 별점까지만 강조)
            stars.forEach(s => s.style.color = s.getAttribute('data-value') <= selectedRating ? 'gold' : 'gray');
        });
    });


    // 폼 제출 시 Ajax 요청
    $("#form-review").submit(function(e) {
        e.preventDefault(); // 기본 폼 제출을 방지

        let prodNo = $("input[name=prodNo]").val(); // 상품 번호
        let colorNo = $("input[name=colorNo]").val(); // 색상 번호
        let userNo = $("input[name=userNo]").val(); // 사용자 번호
        let title = $("input[name=title]").val(); // 제목
        let content = $("textarea[name=content]").val(); // 댓글 내용
        let rating = $("input[name=rating]").val(); // 별점
        let prodName = $("input[name=prodName]").val();// 상품 이름
        let colorName = $("input[name=colorName]").val(); // 색상 이름
        let userNickname = $("input[name=userNickname]").val();
        let files = $("#reviewFile")[0].files; // 파일 목록 가져오기

        // 값이 하나라도 비어 있으면 예외 처리
        if (!title || !content || !rating) {
            alert("모든 리뷰 필드를 작성해주세요.");
            return false; // 폼 제출을 막음
        }

        // rating 최소값 체크
        if (rating < 1 || rating > 5) {
            alert("별점은 최소 1점에서 최대 5점 사이여야 합니다.");
            return false;
        }

        let formData = new FormData();
        formData.append("prodNo", prodNo);
        formData.append("colorNo", colorNo);
        formData.append("userNo", userNo);
        formData.append("title", title);
        formData.append("content", content);
        formData.append("rating", rating);
        formData.append("prodName", prodName);
        formData.append("colorName", colorName);
        formData.append("userNickname", userNickname);


        // 여러 파일을 FormData에 추가한다.
        for(let i = 0; i< files.length; i++) {
            formData.append("reviewFiles", files[i]);
        }

        // Ajax 요청
        $.ajax({
            url: "addProdReview",
            type: 'POST', // HTTP 메서드
            data: formData, // FormData 객체
            processData: false,
            contentType: false,
            success: function(dto) {


                let reviewHtml = `
        <div class="mb-3"></div>
        <div class="comment">
            <div class="row align-items-center">
                <div class="col-3" style="text-align: start;">
                    <img src="https://github.com/mdo.png" alt="프로필 이미지" style="width: 50px; height: 50px;"
                         class="rounded-circle mb-2">

                    <strong>${user.nickname}</strong><br/>

                    <span style="font-size: 0.9rem; color: #555;">\${dto.reviewDate}</span><br/>
                    <span style="font-size: 0.9rem; color: #555;">\${dto.prodName} [\${dto.colorName}]</span>

                    <div class="star-rating" style="font-size: 1.2rem; color: gold;">
                        &#9733;&#9733;&#9733;&#9733;&#9734;
                    </div>
                    <div class="mt-1">
                        <button class="btn btn-outline-secondary">답글</button>
                    </div>
                </div>

                <div class="col">
                    <div class="mt-2">
                `;

                let images = dto.prodReviewImgs;
                    for (img of images) {
                        reviewHtml += `<img src="https://2404-bucket-team-1.s3.ap-northeast-2.amazonaws.com/resources/images/product-review/\${img.imgName}" alt="리뷰 이미지" class="img-fluid" style="max-height: 100px; object-fit: cover;">`;
                    }
                reviewHtml +=  `
                    </div>
                    <p style="margin: 3px;">\${dto.reviewTitle} : \${dto.reviewContent}</p>
                </div>

                <div class="col-2 text-end">
                    <button type="button" class="btn btn-warning btn-sm">수정</button>
                    <button type="button" class="btn btn-danger btn-sm">삭제</button>
                </div>
            </div>
        </div>
        <br>
                `;

                // 새 리뷰를 #wrapper-reviews에 추가
                $("#wrapper-reviews").prepend(reviewHtml);
                location.reload();


            },
            error: function(xhr, status, error) {
                alert("오류가 발생했습니다: " + error);
            }
        });
    });

    let editReviewModal = new bootstrap.Modal(document.getElementById('form-modal'));

    function openEditModal(reviewNo) {
        // 서버에서 리뷰 데이터를 가져오기
        fetch(`reviews/`+reviewNo, {
            method: 'GET',
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(reviewNo +'리뷰 데이터를 불러오는 데 실패했습니다.');
                }
                return response.json();
            })
            .then(data => {
                // 가져온 데이터를 모달 폼에 채우기
                document.querySelector('#form-modal input[name="no"]').value = data.no;
                document.querySelector('#form-modal input[name="title"]').value = data.title;
                document.querySelector('#form-modal textarea[name="content"]').value = data.content;
                setRatingStars(data.rating);

                // 모달 열기
                editReviewModal.show();
            })
            .catch(error => {
                console.error('Error:', error);
                alert(reviewNo+'리뷰 데이터를 불러오지 못했습니다.');
            });
    }

    // 리뷰 데이터에서 가져온 rating 값으로 별을 미리 표시
    function setRatingStars(rating) {
        // 모든 별을 초기화
        $('#commentStarRating-edit .star').each(function() {
            $(this).removeClass('selected'); // 기존에 선택된 별을 제거
        });

        // rating 값에 맞는 별을 선택
        $('#commentStarRating-edit .star').each(function() {
            let starValue = $(this).data('value');
            if (starValue <= rating) {
                $(this).addClass('selected'); // 선택된 별
            }
        });

        // hidden input에 rating 값 설정
        $('#commentRating-edit').val(rating);
    }

    // 별 클릭 시 별점 변경
    $('#commentStarRating-edit .star').on('click', function() {
        let rating = $(this).data('value');

        // 선택된 별을 표시
        setRatingStars(rating);
    });

    // 리뷰 수정
    function submitEditReview() {
        let formData = new FormData($("#editReviewForm")[0]);
        let userNo = $("input[name=userNo]").val();
        formData.append("userNo", userNo);

        $.ajax({
            url: '/product/reviews/edit',
            type: 'POST',
            data: formData,
            contentType: false,  // Content-Type을 자동으로 설정하지 않음
            processData: false,  // 데이터 처리 방식을 자동으로 설정하지 않음
            success: function(response) {
                alert(response); // 성공 메시지 출력
                location.reload(); // 페이지 새로고침 (리뷰 목록 갱신)
            },
            error: function(xhr) {
                alert("리뷰 수정 실패: " + xhr.responseText);
            }
        });
    }

    // 리뷰 삭제
    function deleteReview(reviewNo) {
        if (confirm('정말로 이 리뷰를 삭제하시겠습니까?')) {
            $.ajax({
                url: '/product/review/delete/' + reviewNo,  // 삭제 요청을 보낼 URL
                type: 'POST',  // POST 방식으로 요청
                success: function(response) {
                    if (response.success) {
                        alert('리뷰가 삭제되었습니다.');
                        // 페이지 새로고침 또는 삭제된 항목 제거
                        location.reload(); // 예: 페이지 새로고침
                    } else {
                        alert('리뷰 삭제에 실패했습니다.');
                    }
                },
                error: function(xhr, status, error) {
                    alert('에러 발생: ' + error);
                }
            });
        }
    }
</script>
<%@include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
