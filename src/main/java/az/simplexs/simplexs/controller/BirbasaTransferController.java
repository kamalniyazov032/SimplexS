package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.repository.eczaxana.*;
import az.simplexs.simplexs.security.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/eczaxana/birbasa-transfer")
public class BirbasaTransferController {
    private final BirbasaTransferRepository repo;
    private final AnbarKontekstRepository context;
    private final AccessService access;
    private final MessageSource messages;
    private final ObjectMapper json;
    public BirbasaTransferController(BirbasaTransferRepository repo,AnbarKontekstRepository context,AccessService access,MessageSource messages,ObjectMapper json) {
        this.repo=repo;this.context=context;this.access=access;this.messages=messages;this.json=json;
    }
    private Long scope(Authentication auth,HttpSession session,Long sender) {
        Long clinic=(Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        if(auth==null || !(auth.getPrincipal() instanceof AuthenticatedPersonal personal) || clinic==null || !access.hasClinic(auth,clinic)
                || !repo.canAccess(clinic,personal.personalId())
                || sender!=null && context.warehouses(clinic,personal.personalId()).stream().noneMatch(w->Objects.equals(w.anbarId(),sender)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,messages.getMessage("teleb.denied",null,LocaleContextHolder.getLocale()));
        return clinic;
    }
    @GetMapping
    public String page(Authentication auth,HttpSession session,Model model) {
        Long clinic=scope(auth,session,null);
        model.addAttribute("pageTitle",messages.getMessage("transfer.title",null,LocaleContextHolder.getLocale()));
        model.addAttribute("warehouses",context.warehouses(clinic,((AuthenticatedPersonal)auth.getPrincipal()).personalId()));
        model.addAttribute("receivers",repo.warehouses(clinic));
        model.addAttribute("today",LocalDate.now());
        return "pages/eczaxana/birbasaTransfer";
    }
    @GetMapping("/materiallar") @ResponseBody
    public Object materials(@RequestParam Long anbarId,@RequestParam String tarix,Authentication auth,HttpSession session) {
        scope(auth,session,anbarId);return repo.materials(anbarId,tarix);
    }
    @GetMapping("/vahidler") @ResponseBody
    public Object units(@RequestParam Long anbarId,@RequestParam String tarix,@RequestParam Long materialId,Authentication auth,HttpSession session) {
        scope(auth,session,anbarId);
        if(repo.materials(anbarId,tarix).stream().noneMatch(m->m.get("material_id") instanceof Number n && n.longValue()==materialId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,messages.getMessage("teleb.notFound",null,LocaleContextHolder.getLocale()));
        return repo.units(materialId);
    }
    @PostMapping @ResponseBody @Transactional
    public Object save(@RequestParam Long anbarId,@RequestBody Map<String,Object> data,Authentication auth,HttpSession session) {
        Long clinic=scope(auth,session,anbarId);
        Long receiver;
        try {receiver=Long.valueOf(Objects.toString(data.get("qebul_eden_anbar_id"),""));}
        catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,messages.getMessage("teleb.invalidInput",null,LocaleContextHolder.getLocale()));}
        if(Objects.equals(anbarId,receiver))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,messages.getMessage("transfer.sameWarehouse",null,LocaleContextHolder.getLocale()));
        if(repo.warehouses(clinic).stream().noneMatch(w->w.get("anbar_id") instanceof Number n && n.longValue()==receiver))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,messages.getMessage("teleb.denied",null,LocaleContextHolder.getLocale()));
        if(!(data.get("materiallar") instanceof List<?> items) || items.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,messages.getMessage("teleb.materialsRequired",null,LocaleContextHolder.getLocale()));
        var result=repo.save(anbarId,receiver,Objects.toString(data.get("transfer_tarixi"),null),json.writeValueAsString(items),Objects.toString(data.get("aciqlama"),null),((AuthenticatedPersonal)auth.getPrincipal()).personalId());
        return ResponseEntity.status("UGURLU".equals(result.get("status_kodu"))?200:422).body(result);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> rejected(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.toString(e.getReason(),"")));
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> failure(DataAccessException e) {
        String message=messages.getMessage("teleb.loadFailed",null,LocaleContextHolder.getLocale());
        if(e.getMostSpecificCause() instanceof java.sql.SQLException sql && sql.getMessage()!=null)message=sql.getMessage().split("\\n",2)[0].replaceFirst("^ERROR: *","");
        return ResponseEntity.status(422).body(Map.of("message",message));
    }
}
