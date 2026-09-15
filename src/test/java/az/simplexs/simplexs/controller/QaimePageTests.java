package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.dto.klinika.KlinikaListItem;
import az.simplexs.simplexs.repository.eczaxana.*;
import az.simplexs.simplexs.repository.klinika.KlinikaRepository;
import az.simplexs.simplexs.repository.tercume.TercumeRepository;
import az.simplexs.simplexs.repository.xeta.XetaJurnaliRepository;
import az.simplexs.simplexs.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.flyway.enabled=false"})
@AutoConfigureMockMvc
class QaimePageTests {
    @Autowired MockMvc mvc;
    @MockitoBean DataSource dataSource;
    @MockitoBean org.springframework.transaction.PlatformTransactionManager transactions;
    @MockitoBean AnbarKontekstRepository context;
    @MockitoBean QaimeRepository repo;
    @MockitoBean KlinikaRepository clinics;
    @MockitoBean TercumeRepository translations;
    @MockitoBean XetaJurnaliRepository journal;
    @MockitoBean AccessService access;
    MockHttpSession session;
    @BeforeEach void setup(){
        when(transactions.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        session=new MockHttpSession();var user=new AuthenticatedPersonal(24L,"admin","","Test",true,List.of());
        session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of())));
        session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID,1L);
        when(access.hasFullAccess(any())).thenReturn(true);when(access.hasClinic(any(),any())).thenReturn(true);when(access.clinicIds(any())).thenReturn(List.of(1L));
        var clinic=mock(KlinikaListItem.class);when(clinic.klinikaId()).thenReturn(1L);when(clinic.aktiv()).thenReturn(true);when(clinic.klinikaAdi()).thenReturn("Test klinika");when(clinics.findAll()).thenReturn(List.of(clinic));
        when(context.canAccess(1L,24L)).thenReturn(true);
        when(context.warehouses(1L,24L)).thenReturn(List.of(new AnbarKontekstRepository.Warehouse(2L,"Kardiologiya anbarı",1L,"Əsas",java.time.LocalDate.of(2025,1,1),false,true,"FIFO","FIFO")));
        when(context.years(1L,2L)).thenReturn(List.of(new AnbarKontekstRepository.Year(2026,true,java.time.LocalDate.of(2025,1,1))));
        var system=new MenuSystem(1L,"HIS","Klinika idarəetmə","ti ti-building-hospital");var pharmacy=new MenuModule(10L,null,"HIS_PHARMACY","Əczaxana",null,"ti ti-pill");
        pharmacy.getChildren().add(new MenuModule(11L,10L,"HIS_PHARMACY_INVOICES","Qaimələr",null,"ti ti-file-invoice"));system.getModules().add(pharmacy);when(access.menuSystems(any(),any())).thenReturn(List.of(system));
    }
    @Test void rendersInlineContextAndOffcanvasWithoutContextPopup()throws Exception{
        var result=mvc.perform(get("/eczaxana/qaimeler").session(session)).andExpect(status().isOk()).andReturn();
        var html=result.getResponse().getContentAsString();assertThat(html).contains("qaimeWarehouse","qaimeYear","qaimeSearch","qaimeFilter","qaimeNew","qaimeDetails","Kardiologiya anbarı","href=\"/eczaxana/qaimeler\"");
        assertThat(html).doesNotContain("openWarehouseContext","warehouseContextModal","qaime.title");
        assertThat(html.indexOf("id=\"qaimeWarehouse\"")).isLessThan(html.indexOf("id=\"qaimeYear\""));
        Path out=Path.of("target/qaime-preview.html");Files.createDirectories(out.getParent());Files.writeString(out,html);
        verifyNoInteractions(dataSource);
    }
    @Test void rejectsWarehouseTamperingAndDoesNotQueryYears()throws Exception{
        mvc.perform(get("/eczaxana/qaimeler/iller").param("anbarId","99").session(session)).andExpect(status().isForbidden());
        verify(context,never()).years(any(),any());verifyNoInteractions(dataSource);
    }
    @Test void requiresCsrfForWrites()throws Exception{
        mvc.perform(post("/eczaxana/qaimeler/yeni").param("anbarId","2").param("il","2026").contentType("application/json").content("{}").session(session)).andExpect(status().isForbidden());
        verify(repo,never()).execute(any(),any());
    }
    @Test void postsUseAuthenticatedClinicAndPersonal()throws Exception{
        var page=mvc.perform(get("/eczaxana/qaimeler").session(session)).andReturn();
        var token=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(repo.execute(any(),any())).thenReturn(Map.of("status_kodu","UGURLU","qaime_id",7L));
        mvc.perform(post("/eczaxana/qaimeler/yeni").session(session).header(token.getHeaderName(),token.getToken()).param("anbarId","2").param("il","2026").contentType("application/json")
            .content("{\"qaime_tarixi\":\"2026-09-15\",\"klinika_id\":999,\"yaradan_personal_id\":999,\"materiallar\":[{\"material_id\":1}]}")).andExpect(status().isOk());
        verify(repo).execute(eq(QaimeRepository.Operation.CREATE),argThat(v->Long.valueOf(1).equals(v.get("klinika_id"))&&Long.valueOf(24).equals(v.get("yaradan_personal_id"))));
        verifyNoInteractions(dataSource);
    }
}
