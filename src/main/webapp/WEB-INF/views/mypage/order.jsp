<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/views/common/tags.jsp" %>
<!doctype html>
<html lang="ko">
<head>
    <%@include file="/WEB-INF/views/common/common.jsp" %>
    <style>
        .tab-content {
            display: none; /* 기본적으로 모든 탭 내용을 숨기기 */
        }

        .tab-content.active {
            display: table-row; /* 활성화된 탭 내용만 보이기 */
        }

        .btn-no-style {
            background: none;
            border: none;
            color: inherit;
            text-decoration: none;
            padding: 5px 15px;
            cursor: pointer;
            font-size: 18px; /* 기본 글씨 크기 */
            transition: all 0.3s ease; /* 애니메이션 효과 */
        }

        .btn-no-style:hover {
            transform: scale(1.1); /* hover 시 글씨 크기 확대 */
        }

        .btn-no-style:active,
        .btn-no-style:focus {
            text-decoration: underline; /* 클릭 시 밑줄 */
            font-weight: bold; /* 클릭 시 진하게 표시 */
        }

        .border-right {
            border-right: 1px solid #ccc; /* 버튼 오른쪽에 선 추가 */
            padding-right: 10px; /* 버튼과 선 사이에 여백 */
        }

        .border-left {
            border-left: 1px solid #ccc; /* 버튼 오른쪽에 선 추가 */
            padding-left: 10px; /* 버튼과 선 사이에 여백 */
        }

    </style>
</head>
<body>
<%@include file="/WEB-INF/views/common/nav.jsp" %>
<div class="container-xxl text-center" id="wrap">
    <div>
        <h2>주문결제</h2>
    </div>
    <hr class="bg-primary border border-1">

    <div class="row mb-3">
        <div class="col">
            <table class="table align-middle mt-2 md-2">
                <colgroup>
                    <col width="5%"/>
                    <col width="80%"/>
                    <col width="15%">
                </colgroup>
                <thead class="table-secondary">
                <tr class="text-start">
                    <th colspan="3">주문상품</th>
                </tr>
                </thead>
                <tbody id="order-items">
                <c:forEach items="${orderItems}" var="item">
                    <tr>
                        <td>
                            <img src="${item.imgThum}" class="rounded mx-auto d-block" width="90">
                        </td>
                        <td>
                            <input type="hidden" name="cartNo" data-cartNo="${item.no}">
                            <div data-prodNo="${item.product.no}"></div>
                            <span data-prodName="${item.product.name}">${item.product.name}</span>
                            <p class="text-secondary" id="stock-${item.size.no}" data-sizeNo="${item.size.no}"
                               data-stock="${item.stock}">[${item.color.name}/${item.size.size}] / ${item.stock}개</p>

                        </td>
                        <td class="text-end">
                            <span id="price-${item.size.no}" data-price="${item.product.price}"><fmt:formatNumber
                                    value="${item.product.price * item.stock}"/> 원</span>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    <!--
        배송지 정보
    -->
    <div class="col-md-12 mb-4">
        <table class="table align-middle mt-2">
            <thead class="table-secondary">
            <tr class="text-start">
                <th colspan="3">배송지 정보</th>
            </tr>
            </thead>
            <colgroup>
                <col width="20%"/>
                <col width="50%"/>
                <col width="25%">
            </colgroup>
            <tbody>
            <tr>
                <th colspan="3">
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="fw-bold">신규 입력</span>
                        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addressListModal">
                            배송지목록
                        </button>
                    </div>
                </th>
            </tr>

            <!-- 신규 입력 내용 -->
            <tr class="tab-content active" id="new-tab">
                <td colspan="3">
                    <table class="table">
                        <!-- 신규 입력 필드 -->
                        <tr>
                            <th>받으실 분</th>
                            <td>
                                <input type="text" name="name" class="form-control" placeholder="이름을 입력하세요" required>
                            </td>
                        </tr>
                        <tr>
                            <th>주소</th>
                            <td>
                                <div class="d-flex align-items-center mb-2">
                                    <input type="text" name="postcode" class="form-control me-2" id="postcode"
                                           placeholder="우편번호" required readonly>
                                    <input type="button" onclick="openPostcode()" class="btn btn-secondary"
                                           value="우편번호검색">
                                </div>
                                <input type="text" name="address" id="address" class="form-control mb-2"
                                       placeholder="기본주소"/>
                                <input type="text" name="address-detail" id="address-detail"
                                       class="form-control mb-2" placeholder="나머지 주소(선택입력 가능)"/>
                                <input type="text" name="address-extra" id="address-extra" class="form-control mb-2"
                                       placeholder="참고항목"/>
                            </td>
                        </tr>
                        <tr>
                            <th><label>휴대폰 번호</label></th>
                            <td>
                                <div class="d-flex">
                                    <input type="text" class="form-control" id="phone-number" name="phone-number">
                                    <div id="phone-error" style="color: red; font-size: 12px; display: none;"></div>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <th><label>이메일</label></th>
                            <td>
                                <div class="d-flex">
                                    <input type="email" id="email-user-name" name="email-user-name"
                                           class="form-control" oninput="validateEmail()"> @
                                    <input type="email" id="email-domain" class="form-control" name="email-domain"
                                           disabled oninput="validateEmail()">
                                    <select id="email-selector" class="form-control" name="email"
                                            onchange="updateEmailDomain()" oninput="validateEmail()">
                                        <option>선택하세요</option>
                                        <option value="direct">직접입력</option>
                                        <option value="gmail.com">gmail.com</option>
                                        <option value="naver.com">naver.com</option>
                                        <option value="hanmail.net">hanmail.net</option>
                                    </select>
                                </div>
                                <div id="error-message" style="color: red; display: none;">유효하지 않은 이메일 주소입니다.</div>
                            </td>
                        </tr>
                        <tr>
                            <th><label class="form-check-label" for="memo-box">배송 메모</label></th>
                            <td>
                                <select id="memo-box" class="form-control" name="memo" aria-labelledby="memo-label">
                                    <option value="" selected>-- 메시지 선택 --</option>
                                    <option value="배송 전에 미리 연락바랍니다.">배송 전에 미리 연락바랍니다.</option>
                                    <option value="부재 시 경비실에 맡겨주세요.">부재 시 경비실에 맡겨주세요.</option>
                                    <option value="부재 시 문 앞에 놓아주세요.">부재 시 문 앞에 놓아주세요.</option>
                                    <option value="빠른 배송 부탁드립니다.">빠른 배송 부탁드립니다.</option>
                                    <option value="택배함에 보관해주세요.">택배함에 보관해주세요.</option>
                                    <option value="direct">직접입력</option>
                                </select>
                                <input type="text" class="form-control" id="memo-box-direct" name="memo-direct"
                                       placeholder="배송 메모를 입력하세요"/>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            </tbody>
        </table>

        <!-- 배송지 모달 창 -->
        <div class="modal fade" id="addressListModal" tabindex="-1" aria-labelledby="addressListModalLabel"
             style="display: none;" role="dialog">
            <div class="modal-dialog modal-lg" style="max-width: 30%;">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="addressListModalLabel">최근 배송지</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body" style="max-height: 600px; overflow-y: auto;">
                        <!-- 주소 목록을 동적으로 생성 -->
                        <c:forEach var="address" items="${addresses}">
                            <div class="card mb-2">
                                <div class="card-body text-start" style="line-height: 1.4; padding: 10px;">
                                    <!-- 라디오 버튼 -->
                                    <input type="radio" id="address${address.no}" name="selectedAddress"
                                           value="${address.no}"
                                           class="form-check-input me-2"
                                           onclick="fillCartForm('${address.name}', '${address.postcode}', '${address.address}', '${address.addressDetail}', '${address.isAddrHome}')">
                                    <label class="form-check-label" for="address${address.no}">
                                        <strong>${address.name}</strong>
                                    </label>
                                    <!-- 주소 정보 출력 -->
                                    <p style="margin: 5px 0;"><strong>받으실 분:</strong> ${address.name}</p>
                                    <p style="margin: 5px 0;"><strong>(우편번호) 주소:</strong>
                                        (${address.postcode}) ${address.address}</p>
                                    <p style="margin: 5px 0;"><strong>상세 주소:</strong> ${address.addressDetail}</p>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                    <div class="modal-footer">
                        <div class="d-flex justify-content-end w-100">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>


    </div>
    <!--
        쿠폰 적립금
    -->
    <div class="row mb-3">
        <div class="col">
            <table class="table align-middle mt-2 md-2">
                <colgroup>
                    <col width="5%"/>
                    <col width="50%"/>
                    <col width="45%">
                </colgroup>
                <thead class="table-secondary">
                <tr class="text-start">
                    <th colspan="3">쿠폰 적립금</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <th><label>쿠폰</label></th>
                    <td>
                        <input type="text" class="form-control mb-3" value="신입 러너들 10% 할인" disabled>
                    </td>
                </tr>
                <tr>
                    <th><label>적립금</label></th>
                    <td>
                        <div class="d-flex align-items-center mb-3">
                            <input type="text" class="form-control me-2" disabled/>
                            <input type="button" class="btn btn-secondary" value="모두 사용" disabled>
                        </div>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
    <!--
    결제 정보
    -->
    <div class="row mb-3">
        <div class="col">
            <table class="table align-middle mt-2 md-2">
                <thead class="table-secondary">
                <tr class="text-start">
                    <th colspan="12">결제 정보</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <th><label>결제 수단</label></th>
                    <td><input type="radio" name="paymentMethod" value="신용카드"><img
                            src="https://ecimg.cafe24img.com/pg90b05313110010/brooksrunning/web/upload/icon_202210121023113100.png"/>
                    </td>
                    <td><input type="radio" name="paymentMethod" value="카카오페이" checked><img
                            src="https://ecimg.cafe24img.com/pg90b05313110010/brooksrunning/web/upload/icon_202210121022402200.png"/>
                    </td>
                    <td><input type="radio" name="paymentMethod" value="페이코"><img
                            src="https://img.echosting.cafe24.com/design/skin/admin/ko_KR/ico_payco_disabled.gif"/>
                    </td>
                    <td><input type="radio" name="paymentMethod" value="네이버페이"><img
                            src="https://img.echosting.cafe24.com/skin/admin_ko_KR/order/admin_naverpay_disabled.gif"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
    <hr class="bg-dark border-dark border-4">
    <div class="row">
        <div class="col">
            <table class="table table-borderless">
                <colgroup>
                    <col width="50%">
                    <col width="50%">
                </colgroup>
                <thead class="table-secondary">
                <tr class="text-start">
                    <th colspan="2">주문예정 금액</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="text-start">상품금액</td>
                    <td class="text-end" id="total-price"> 0 원</td>
                </tr>
                <tr>
                    <td class="text-start">배송비</td>
                    <td class="text-end" id="delivery-price">0 원</td>
                </tr>
                <tr>
                    <td class="text-start">총 할인금액</td>
                    <td class="text-end" id="discount-price">0 원</td>
                </tr>
                <tr>
                    <td class="text-start">
                        <strong>총결제 금액</strong>
                    </td>
                    <td class="text-end">
                        <strong class="text-danger fs-4" id="final-total-price"> 0 원</strong>
                    </td>
                </tr>
                </tbody>
            </table>
            <div class="d-grid gap-2">
                <button class="col btn btn-dark" type="button" id="cancelOrderBtn">주문취소하기</button>
                <button class="col btn btn-dark" id="submit-button" type="button">결제하기 <small
                        id="total-quantity"></small></button>
            </div>
        </div>
    </div>
</div>

<!--
    주문 취소 모달
-->
<div class="modal fade" id="cancelModal" tabindex="-1" aria-labelledby="cancelModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="cancelModalLabel">주문 취소</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                정말 주문을 취소하시겠습니까?
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary">확인</button>
            </div>
        </div>
    </div>
</div>
<!--
    결제
-->
<div class="modal fade" id="paymentModal" tabindex="-1" aria-labelledby="paymentModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="paymentModalLabel">결제 확인</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p>총 결제 금액: <span id="totalAmount"></span> 원</p>
                <p>결제를 진행하시겠습니까?</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" id="payBtn">결제</button>
            </div>
        </div>
    </div>
</div>

<script>
    // 라디오 버튼 클릭 시 선택된 주소 정보 입력
    function fillCartForm(name, postcode, address, addressDetail, isAddrHome) {
        // 주소 정보가 입력될 폼 필드
        document.querySelector("[name='name']").value = name;
        document.querySelector("[name='postcode']").value = postcode;
        document.querySelector("[name='address']").value = address;
        document.querySelector("[name='address-detail']").value = addressDetail;
    }

    // 수정 버튼 클릭 시
    function editAddress() {
        if (selectedAddressNo) {
            // 수정 페이지로 이동하거나, 모달 내에 수정 폼을 띄우는 로직 추가
            window.location.href = "/address/update?addrNo=" + selectedAddressNo; // 수정 페이지로 리디렉션
        } else {
            alert("주소를 선택해주세요.");
        }
    }

    // 삭제 버튼 클릭 시
    function deleteAddress() {
        if (selectedAddressNo) {
            if (confirm("정말로 삭제하시겠습니까?")) {
                // 삭제 요청을 서버로 보내는 로직
                window.location.href = "/address/delete?addrNo=" + selectedAddressNo; // 삭제 요청
            }
        } else {
            alert("주소를 선택해주세요.");
        }
    }


    // 카카오 주소 api
    function openPostcode() {
        new daum.Postcode({
            oncomplete: function (data) {
                // 팝업에서 검색결과 항목을 클릭했을때 실행할 코드를 작성하는 부분.

                // 각 주소의 노출 규칙에 따라 주소를 조합한다.
                // 내려오는 변수가 값이 없는 경우엔 공백('')값을 가지므로, 이를 참고하여 분기 한다.
                var addr = ''; // 주소 변수
                var extraAddr = ''; // 참고항목 변수

                //사용자가 선택한 주소 타입에 따라 해당 주소 값을 가져온다.
                if (data.userSelectedType === 'R') { // 사용자가 도로명 주소를 선택했을 경우
                    addr = data.roadAddress;
                } else { // 사용자가 지번 주소를 선택했을 경우(J)
                    addr = data.jibunAddress;
                }

                // 사용자가 선택한 주소가 도로명 타입일때 참고항목을 조합한다.
                if (data.userSelectedType === 'R') {
                    // 법정동명이 있을 경우 추가한다. (법정리는 제외)
                    // 법정동의 경우 마지막 문자가 "동/로/가"로 끝난다.
                    if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)) {
                        extraAddr += data.bname;
                    }
                    // 건물명이 있고, 공동주택일 경우 추가한다.
                    if (data.buildingName !== '' && data.apartment === 'Y') {
                        extraAddr += (extraAddr !== '' ? ', ' + data.buildingName : data.buildingName);
                    }
                    // 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
                    if (extraAddr !== '') {
                        extraAddr = ' (' + extraAddr + ')';
                    }
                    // 조합된 참고항목을 해당 필드에 넣는다.
                    document.getElementById("address-extra").value = extraAddr;

                } else {
                    document.getElementById("address-extra").value = '';
                }

                // 우편번호와 주소 정보를 해당 필드에 넣는다.
                document.getElementById('postcode').value = data.zonecode;
                document.getElementById("address").value = addr;
                // 커서를 상세주소 필드로 이동한다.
                document.getElementById("address-detail").focus();
            }
        }).open();
    }

    $(document).ready(function () {
        //
        $("#memo-box-direct").hide();

        // 변경 이벤트 처리
        $("#memo-box").change(function () {
            if ($(this).val() === "direct") {
                $("#memo-box-direct").show();
            } else {
                $("#memo-box-direct").hide().val("");
            }
        });
    });

    // 핸드폰 번호 정규식
    $(document).on("focus", "#phone-number", function () {
        // 입력 시 자동으로 010을 채워넣기
        if ($(this).val() === "") {
            $(this).val("010");
        }
    });

    $(document).on("keyup", "#phone-number", function () {
        var phoneNumber = $(this).val().replace(/[^0-9]/g, "");  // 숫자만 남기기

        // 입력이 비어있으면 에러 메시지 제거
        if (phoneNumber === "") {
            $("#phone-error").text("").hide();  // 에러 메시지 숨기기
            return;
        }

        // 전화번호가 010으로 시작하는지 체크
        if (phoneNumber.startsWith("010")) {
            // 길이가 10자리 이상이고 11자리 이하일 때만 유효
            if (phoneNumber.length >= 10 && phoneNumber.length <= 11) {
                // 010으로 시작하면 포맷팅: 010-xxxx-xxxx
                $(this).val(
                    phoneNumber
                        .replace(/^(\d{3})(\d{1,4})(\d{0,4})$/, function (_, p1, p2, p3) {
                            if (p3) {
                                return p1 + '-' + p2 + '-' + p3;  // 010-xxxx-xxxx 형식
                            } else if (p2) {
                                return p1 + '-' + p2;  // 010-xxxx 형식
                            } else {
                                return p1;  // 010만 있을 경우
                            }
                        })
                );

                // 에러 메시지 숨기기
                $("#phone-error").text("").hide();
            } else {
                // 길이가 잘못된 경우, 에러 메시지 표시
                $(this).val(phoneNumber);  // 숫자만 남기고 입력

                // 에러 메시지가 이미 있으면 새로 추가하지 않도록 처리
                if ($("#phone-error").length === 0) {
                    $(this).after('<div id="phone-error" style="color: red; font-size: 12px;"></div>');
                }

                // 에러 메시지 업데이트
                $("#phone-error").text("전화번호는 010-xxxx-xxxx 형식이어야 합니다.").show();
            }
        } else {
            // 010으로 시작하지 않으면 잘못된 입력으로 처리
            $(this).val(phoneNumber);  // 숫자만 남기고 입력

            // 에러 메시지가 이미 있으면 새로 추가하지 않도록 처리
            if ($("#phone-error").length === 0) {
                $(this).after('<div id="phone-error" style="color: red; font-size: 12px;"></div>');
            }

            // 에러 메시지 업데이트
            $("#phone-error").text("올바른 전화번호 형식(010-xxxx-xxxx)을 입력해주세요.").show();
        }
    });

    // 이메일 유효성 검사 함수
    function isValidEmail(email) {
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        return emailRegex.test(email);
    }

    // 이메일 도메인 갱신 함수
    function updateEmailDomain() {
        const domainSelector = document.getElementById('email-selector');
        const emailDomainInput = document.getElementById('email-domain');

        if (domainSelector.value === 'direct') {
            emailDomainInput.disabled = false;  // 사용자가 직접 입력할 수 있도록 활성화
            emailDomainInput.placeholder = '도메인 입력';
            emailDomainInput.value = '';  // 기존 선택된 값 지우기
        } else {
            emailDomainInput.disabled = true;  // 도메인 입력을 비활성화
            emailDomainInput.value = domainSelector.value;  // 선택한 도메인 자동 입력
        }
        validateEmail();  // 도메인 갱신 후 즉시 유효성 검사
    }

    // 실시간 이메일 유효성 검사 함수
    function validateEmail() {
        const emailUsername = document.getElementById('email-user-name').value;
        const emailDomain = document.getElementById('email-domain').value;
        const emailSelector = document.getElementById('email-selector');
        const errorMessage = document.getElementById('error-message');

        // 이메일을 완성하기
        const fullEmail = emailUsername + '@' + (emailDomain || emailSelector.value);

        // 이메일 입력이 비어있거나, 이메일 형식이 잘못되었을 경우 에러 메시지 숨기기
        if (!emailUsername || !fullEmail || !isValidEmail(fullEmail)) {
            errorMessage.style.display = 'none';  // 이메일이 비어있거나 형식이 잘못되면 에러 메시지 숨기기
            return;
        }

        // 이메일 유효성 검사
        if (isValidEmail(fullEmail)) {
            errorMessage.style.display = 'block';  // 에러 메시지 표시
            errorMessage.textContent = '유효한 이메일 주소입니다.';  // 유효한 이메일 메시지
            errorMessage.style.color = 'green';  // 글씨 색을 초록색으로 변경
        } else {
            errorMessage.style.display = 'block';  // 에러 메시지 표시
            errorMessage.textContent = '유효하지 않은 이메일 주소입니다.';  // 유효하지 않은 이메일 메시지
            errorMessage.style.color = 'red';  // 글씨 색을 빨간색으로 변경
        }
    }

    $(document).ready(function () {
        // 초기 상태 설정 (기본적으로 신규 입력 탭을 활성화)
        $('#new-tab').addClass('active');
        $('#latest-tab').removeClass('active');

        // 버튼 클릭 시 탭 전환
        $('.btn-no-style').click(function () {
            const tabId = $(this).text() === '최신 배송지' ? '#latest-tab' : '#new-tab';

            // 모든 탭 내용 숨기고 선택된 탭만 보이게
            $('.tab-content').removeClass('active');
            $(tabId).addClass('active');
        });
    });

    // 총 계산
    $(document).ready(function () {
        // 기본 변수 초기화
        let totalPrice = 0; // 주문 상품들 총 가격
        let totalQuantity = 0; // 총 수량
        let deliveryPrice = 0; // 배송비 초기값
        let discountPrice = 0; // 할인 금액

        // 각 아이템에 대해 반복 처리
        $('#order-items tr').each(function () {
            // 각 아이템의 가격과 재고 가져오기
            let price = parseInt($(this).find('[id^="price-"]').data('price'));  // 가격
            let stock = parseInt($(this).find('[id^="stock-"]').data('stock'));  // 재고

            // 상품의 총 금액 계산
            let itemTotalPrice = price * stock;
            totalPrice += itemTotalPrice;  // 총 금액 합산

            totalQuantity += stock;
        });

        // 배송비 계산 (50,000원 이상이면 배송비 면제)
        if (totalPrice >= 50000) {
            deliveryPrice = 0; // 배송비 면제
        } else {
            deliveryPrice = 3000; // 예시 배송비 3000원
        }

        // 할인금액 계산 (예시: 할인율 10%)
        discountPrice = totalPrice * 0.1;

        // 최종 결제 금액 계산
        let finalTotalPrice = totalPrice + deliveryPrice - discountPrice;

        // HTML에 값 설정
        $('#total-price').text(totalPrice.toLocaleString() + ' 원');
        $('#delivery-price').text(deliveryPrice.toLocaleString() + ' 원');
        $('#discount-price').text(discountPrice.toLocaleString() + ' 원');
        $('#final-total-price').text(finalTotalPrice.toLocaleString() + ' 원');
        $('#total-quantity').text(`(` + totalQuantity + ' 개)');
    });


    // 주문
    $(document).ready(function () {
        // 결제 버튼 클릭 이벤트
        $("#submit-button").click(function () { // #submit-button을 클릭했을 때 실행
            const orderData = {
                orderItems: [], // 주문 상품들
            };


            const orderItem = [];
            // 주문 상품 정보 가져오기
            $("#order-items tr").each(function () {
                const item = {
                    cartNo: $(this).find("[data-cartNo]").data("cartno"),
                    prodNo: $(this).find("[data-prodNo]").data("prodno"),
                    prodName: $(this).find("[data-prodName]").data("prodname"),
                    sizeNo: $(this).find('[data-sizeNo]').data('sizeno'),
                    stock: parseInt($(this).find('[data-stock]').data('stock')) || 0,
                    price: parseInt($(this).find('[data-price]').data('price')) || 0,
                };
                orderItem.push(item);
            });
            orderData.orderItems = orderItem;
            console.log("Order Items:", orderItem);

            // 배송지 정보 가져오기
            orderData.recipientName = $("input[name='name']").val();
            orderData.postcode = $("input[name='postcode']").val();
            orderData.address = $("input[name='address']").val();
            orderData.addressDetail = $("input[name='address-detail']").val();
            orderData.phoneNumber = $("input[name='phone-number']").val();
            orderData.email = $("input[name='email-user-name']").val() + "@" + $("input[name='email-domain']").val();
            orderData.memo = $("#memo-box").val();

            console.log("Delivery Info:", orderData);

            // 결제 정보 수집
            orderData.paymentMethod = $("input[name='paymentMethod']:checked").val();

            console.log("Payment Info:", orderData);

            // 총 계산
            let totalPrice = 0; // 주문 상품들 총 가격
            let totalQuantity = 0; // 총 수량
            let deliveryPrice = 0; // 배송비 초기값
            let discountPrice = 0; // 할인 금액

            $('#order-items tr').each(function () {
                // 각 아이템의 가격과 수량 가져오기
                let price = parseInt($(this).find('[id^="price-"]').data('price')) || 0;
                let stock = parseInt($(this).find('[id^="stock-"]').data('stock')) || 0;

                // 상품의 총 금액 계산
                let itemTotalPrice = price * stock;
                totalPrice += itemTotalPrice;
                totalQuantity += stock;
            });

            // 배송비 계산 (50,000원 이상이면 배송비 면제)
            if (totalPrice >= 50000) {
                deliveryPrice = 0; // 배송비 면제
            } else {
                deliveryPrice = 3000; // 예시 배송비 3000원
            }

            // 할인금액 계산 (예시: 할인율 10%)
            discountPrice = totalPrice * 0.1;

            // 최종 결제 금액 계산
            let finalTotalPrice = totalPrice + deliveryPrice - discountPrice;

            // 가격 정보 저장
            orderData.totalPrice = totalPrice;
            orderData.deliveryPrice = deliveryPrice;
            orderData.discountPrice = discountPrice;
            orderData.finalTotalPrice = finalTotalPrice;
            orderData.totalQuantity = totalQuantity;
            orderData.quantity = totalQuantity;
            orderData.type = "상품";

            console.log("Final Order Data:", orderData);

            // 모달 열기
            $('#paymentModal').modal('show');

            const formattedPrice = finalTotalPrice.toLocaleString();
            // 모달 내 총 결제 금액 표시
            $('#totalAmount').text(formattedPrice);

            // 카카오페이 버튼 클릭 이벤트
            $('#payBtn').click(function () {
                // 카카오페이 결제 요청
                $.ajax({
                    url: '/pay/ready',
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(orderData),
                    success: function (response) {
                        // 카카오페이 결제 승인 URL을 받아와 결제 페이지로 이동
                        if (response.next_redirect_pc_url) {
                            location.href = response.next_redirect_pc_url;
                        } else {
                            alert("결제 준비 중 오류가 발생했습니다.");
                        }
                    },
                    error: function (xhr, status, error) {
                        alert('결제 요청 실패: ' + error);
                    }
                });
            });
        });
    });

    // 주문 취소 버튼 클릭시
    $(document).ready(function () {
        $('#cancelOrderBtn').click(function () {
            if (confirm('정말 주문을 취소하시겠습니까?')) {
                window.location.href = '/mypage/cart';
            }
        });
    });

    // 주문상품이 없으면
    $(document).ready(function () {
        // 주문 상품이 있는지 확인
        if ($("#order-items tr").length === 0) {
            alert("주문 상품이 없습니다. 장바구니로 이동합니다.");
            window.location.href = "/mypage/cart"; // 장바구니 페이지 URL
        }
    });

</script>

<%@include file="/WEB-INF/views/common/footer.jsp" %>
</body>
</html>
