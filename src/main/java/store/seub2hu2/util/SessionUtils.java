package store.seub2hu2.util;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionUtils {

    private final HttpSession session;

    public void addAttribute(String name, Object value) {
        session.setAttribute(name, value);
    }

    public <T> T getAttribute(String name) {
        return (T) session.getAttribute(name);
    }

    public void removeAttribute(String name) {
        session.removeAttribute(name);
    }

    public String getSessionId() {
        return session.getId();
    }

    public void invalidateSession() {
        session.invalidate();
    }

    public void clearPaymentSession() {
        removeAttribute("tid");
        removeAttribute("paymentUserId");
        removeAttribute("paymentType");
        log.info("결제 세션 정보 정리 완료");
    }
}
