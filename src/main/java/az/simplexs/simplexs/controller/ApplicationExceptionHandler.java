package az.simplexs.simplexs.controller;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLTimeoutException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import az.simplexs.simplexs.config.RequestCorrelationFilter;
import az.simplexs.simplexs.repository.xeta.XetaJurnaliRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;

@ControllerAdvice
public class ApplicationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApplicationExceptionHandler.class);
    private final XetaJurnaliRepository xetaJurnaliRepository;

    public ApplicationExceptionHandler(XetaJurnaliRepository xetaJurnaliRepository) {
        this.xetaJurnaliRepository = xetaJurnaliRepository;
    }

    private static final String APPLICATION_PACKAGE = "az.simplexs.simplexs.";
    private static final String ERROR_CODE_MDC_KEY = "errorCode";

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ModelAndView handleDatabaseError(Exception exception, HttpServletRequest request) {
        String reference = reference();
        boolean connectionError = isConnectionError(exception);
        logException(reference, "DATABASE_ERROR", exception, request);

        if (!connectionError) {
            journalIfAvailable(reference, "DB_XETASI", exception, request);
        }

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
        logException(reference, "SYSTEM_ERROR", exception, request);
        journalIfAvailable(reference, "SISTEM_XETASI", exception, request);
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

    private void logException(String errorCode, String type, Throwable error, HttpServletRequest request) {
        Throwable finalCause = rootCause(error);
        List<Throwable> chain = new ArrayList<>();
        List<Throwable> additionalTraces = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        var pending = new ArrayDeque<Throwable>();
        pending.push(error);
        while (!pending.isEmpty()) {
            Throwable current = pending.pop();
            if (!visited.add(current)) continue;
            chain.add(current);
            if (current.getCause() != null) pending.push(current.getCause());
            if (current instanceof SQLException sql && sql.getNextException() != null) {
                pending.push(sql.getNextException());
                additionalTraces.add(sql.getNextException());
            }
            for (Throwable suppressed : current.getSuppressed()) pending.push(suppressed);
            if (current instanceof TransactionSystemException transaction
                    && transaction.getApplicationException() != null) {
                pending.push(transaction.getApplicationException());
                additionalTraces.add(transaction.getApplicationException());
            }
        }
        Throwable primary = finalCause;
        Throwable timeout = null;
        Throwable connection = null;
        Throwable sqlCause = null;
        boolean database = false;
        StringBuilder exceptionChain = new StringBuilder();
        for (Throwable cause : chain) {
            if (!exceptionChain.isEmpty()) exceptionChain.append("\n-> ");
            exceptionChain.append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) exceptionChain.append(": ").append(safe(cause.getMessage()));
            String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
            if (cause instanceof DataAccessException || cause instanceof SQLException) database = true;
            if (cause instanceof SocketTimeoutException || cause instanceof SQLTimeoutException
                    || cause instanceof org.springframework.dao.QueryTimeoutException
                    || cause instanceof SQLException && (message.contains("statement timeout") || message.contains("read timed out"))) timeout = cause;
            if (cause instanceof SocketException || cause instanceof SQLException sql
                    && ((sql.getSQLState() != null && sql.getSQLState().startsWith("08"))
                        || message.contains("connection is closed"))) connection = cause;
            if (cause instanceof SQLException) sqlCause = cause;
        }
        String category = "SYSTEM_ERROR";
        if (database && timeout != null) { category = "DB_TIMEOUT"; primary = timeout; }
        else if (database && connection != null) { category = "DB_CONNECTION"; primary = connection; }
        else if (database) { category = "DB_SQL"; if (sqlCause != null) primary = sqlCause; }
        ApplicationLocation location = applicationLocation(primary);
        if ("UNKNOWN".equals(location.className())) location = applicationLocation(error);
        String requestId = requestId(request);
        StringWriter stackTrace = new StringWriter();
        PrintWriter writer = new PrintWriter(stackTrace);
        error.printStackTrace(writer);
        Set<Throwable> printed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable additional : additionalTraces) {
            if (additional != error && printed.add(additional)) {
                writer.println("Additional original/SQL exception:");
                additional.printStackTrace(writer);
            }
        }
        String summary = """
                ==================================================
                ERROR_CODE      : %s
                REQUEST_ID      : %s
                METHOD          : %s
                PATH            : %s
                USER            : %s
                TYPE            : %s

                CATEGORY        : %s
                CLASS           : %s
                FUNCTION        : %s
                LINE            : %s

                EXCEPTION       : %s
                PRIMARY_CAUSE   : %s
                FINAL_CAUSE     : %s

                EXCEPTION_CHAIN :
                %s

                STACK_TRACE:
                %s
                ==================================================
                """.formatted(
                    safe(errorCode), safe(requestId), safe(request.getMethod()), safe(request.getRequestURI()),
                    safe(username()), safe(type), category, safe(location.className()), safe(location.methodName()),
                    location.line() > 0 ? location.line() : "UNKNOWN", error.getClass().getSimpleName(),
                    primary.getClass().getSimpleName() + ": " + safe(rootMessage(primary)),
                    finalCause.getClass().getSimpleName() + ": " + safe(rootMessage(finalCause)), exceptionChain, stackTrace);

        String previousRequestId = MDC.get(RequestCorrelationFilter.MDC_KEY);
        if (previousRequestId == null) MDC.put(RequestCorrelationFilter.MDC_KEY, requestId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(ERROR_CODE_MDC_KEY, errorCode)) {
            log.error(summary);
        } finally {
            if (previousRequestId == null) MDC.remove(RequestCorrelationFilter.MDC_KEY);
        }
    }

    private void journalIfAvailable(String reference, String type, Throwable error, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            Long clinicId = session == null ? null
                    : (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            Long personalId = authentication != null && authentication.getPrincipal() instanceof AuthenticatedPersonal p
                    ? p.personalId() : null;
            String username = authentication == null ? null : authentication.getName();
            Throwable cause = rootCause(error);
            xetaJurnaliRepository.write(reference,type,personalId,username,clinicId,
                    request.getRequestURI(),request.getMethod(),request.getRemoteAddr(),
                    cause.getClass().getName(),cause.getMessage());
        } catch (RuntimeException journalError) {
            // Jurnal DB-yə yazılmasa belə əsas xəta kodu server logunda qorunur.
            log.warn("Xəta kodu {} üçün DB jurnal qeydi yazılmadı", reference, journalError);
        }
    }

    static Throwable rootCause(Throwable error) {
        Throwable result = error;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(result);
        while (result.getCause() != null && visited.add(result.getCause())) result = result.getCause();
        return result;
    }

    static ApplicationLocation applicationLocation(Throwable error) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ApplicationLocation fallback = new ApplicationLocation("UNKNOWN", "UNKNOWN", -1);
        for (Throwable current = error; current != null && visited.add(current); current = current.getCause()) {
            for (StackTraceElement frame : current.getStackTrace()) {
                String className = frame.getClassName();
                if (className.startsWith(APPLICATION_PACKAGE)
                        && !className.equals(ApplicationExceptionHandler.class.getName())) {
                    int separator = className.lastIndexOf('.');
                    String simpleName = className.substring(separator + 1).split("\\$\\$", 2)[0];
                    if (className.contains("$$") || frame.getLineNumber() < 0) {
                        if ("UNKNOWN".equals(fallback.className()))
                            fallback = new ApplicationLocation(simpleName, frame.getMethodName(), -1);
                        continue;
                    }
                    return new ApplicationLocation(simpleName, frame.getMethodName(), frame.getLineNumber());
                }
            }
        }
        return fallback;
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE);
        if (value instanceof String id && !id.isBlank()) return id;
        String mdcValue = MDC.get(RequestCorrelationFilter.MDC_KEY);
        return mdcValue == null || mdcValue.isBlank() ? RequestCorrelationFilter.newId() : mdcValue;
    }

    private String username() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "ANONYMOUS" : authentication.getName();
    }

    private static String rootMessage(Throwable root) {
        return root.getMessage() == null || root.getMessage().isBlank()
                ? root.getClass().getName() : root.getMessage();
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        String sanitized = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        return sanitized.length() <= 2000 ? sanitized : sanitized.substring(0, 2000) + "…";
    }

    record ApplicationLocation(String className, String methodName, int line) {}
}
