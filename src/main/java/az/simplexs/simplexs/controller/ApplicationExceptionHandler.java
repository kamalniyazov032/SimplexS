package az.simplexs.simplexs.controller;

import java.net.SocketException;
import java.sql.SQLException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ApplicationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView handleDatabaseError(DataAccessException exception, HttpServletRequest request) {
        String reference = reference();
        boolean connectionError = isConnectionError(exception);
        log.error("Xəta kodu {} - {} sorğusunda verilənlər bazası xətası", reference,
                request.getRequestURI(), exception);

        if (connectionError) {
            return errorPage(HttpStatus.SERVICE_UNAVAILABLE, reference,
                    "Verilənlər bazası ilə əlaqə qurulmadı",
                    "Sistem verilənlər bazası ilə əlaqəni müvəqqəti itirib. Məlumatlarınızın silinməsi demək deyil.",
                    "Bir neçə saniyə gözləyib səhifəni yenidən açın. Problem davam edərsə, aşağıdakı xəta kodunu sistem administratoruna və ya texniki dəstəyə bildirin.",
                    "ti-database-off");
        }

        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, reference,
                "Məlumat əməliyyatı tamamlanmadı",
                "Verilənlər bazasında əməliyyat aparılarkən gözlənilməz problem yarandı.",
                "Əməliyyatı təkrar yoxlayın. Problem davam edərsə, aşağıdakı xəta kodunu sistem administratoruna və ya texniki dəstəyə bildirin.",
                "ti-database-exclamation");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedError(Exception exception, HttpServletRequest request) {
        String reference = reference();
        log.error("Xəta kodu {} - {} sorğusunda gözlənilməz xəta", reference,
                request.getRequestURI(), exception);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, reference,
                "Gözlənilməz sistem xətası",
                "Sorğunu yerinə yetirərkən gözlənilməz problem yarandı.",
                "Səhifəni yeniləyib yenidən cəhd edin. Problem davam edərsə, aşağıdakı xəta kodunu sistem administratoruna və ya texniki dəstəyə bildirin.",
                "ti-alert-triangle");
    }

    private ModelAndView errorPage(HttpStatus status, String reference, String title,
            String description, String action, String icon) {
        ModelAndView view = new ModelAndView("error/friendly-error");
        view.setStatus(status);
        view.addObject("pageTitle", title);
        view.addObject("statusCode", status.value());
        view.addObject("errorTitle", title);
        view.addObject("errorDescription", description);
        view.addObject("errorAction", action);
        view.addObject("errorReference", reference);
        view.addObject("errorIcon", icon);
        return view;
    }

    private boolean isConnectionError(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketException) {
                return true;
            }
            if (cause instanceof SQLException sqlException
                    && sqlException.getSQLState() != null
                    && sqlException.getSQLState().startsWith("08")) {
                return true;
            }
        }
        return false;
    }

    private String reference() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
