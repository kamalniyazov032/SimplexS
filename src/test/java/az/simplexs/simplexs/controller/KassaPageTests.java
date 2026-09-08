package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.dto.klinika.KlinikaListItem;
import az.simplexs.simplexs.repository.kassa.KassaEmeliyyatRepository;
import az.simplexs.simplexs.repository.klinika.KlinikaRepository;
import az.simplexs.simplexs.repository.tercume.TercumeRepository;
import az.simplexs.simplexs.repository.xeta.XetaJurnaliRepository;
import az.simplexs.simplexs.security.AccessService;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import az.simplexs.simplexs.service.kassa.KassaEmeliyyatService;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.flyway.enabled=false"})
@AutoConfigureMockMvc
class KassaPageTests {
    @Autowired MockMvc mvc;
    @MockitoBean DataSource dataSource; // No live database connections, including error journaling.
    @MockitoBean KassaEmeliyyatRepository repo;
    @MockitoBean KassaEmeliyyatService service;
    @MockitoBean KlinikaRepository clinics;
    @MockitoBean TercumeRepository translations;
    @MockitoBean XetaJurnaliRepository journal;
    @MockitoBean AccessService access;
    MockHttpSession session;
    Map<String,Object> cash=Map.of("kassa_id",7L,"kassa_kodu","KASSA-01","kassa_adi","Mərkəzi Kassa","islesin",true,"izlesin",true);
    static Map<String,Object> row(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]);return result;
    }
    @BeforeEach void setup() {
        session=new MockHttpSession();
        var personal=new AuthenticatedPersonal(24L,"cashier","","Test Cashier",true,List.of());
        session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(personal,null,List.of())));
        session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID,1L);
        when(access.hasFullAccess(any())).thenReturn(true);when(access.clinicIds(any())).thenReturn(List.of(1L));
        var clinic=mock(KlinikaListItem.class);when(clinic.klinikaId()).thenReturn(1L);when(clinic.aktiv()).thenReturn(true);when(clinic.klinikaAdi()).thenReturn("Test klinika");when(clinics.findAll()).thenReturn(List.of(clinic));
        when(service.cashRegisters(any(),eq(1L))).thenReturn(List.of(cash));when(service.requireCash(any(),eq(1L),eq(7L),anyBoolean())).thenReturn(cash);when(service.canBorrow(any(),eq(1L))).thenReturn(true);
        when(repo.visits(1L,7L)).thenReturn(List.of(row("gelis_id",17L,"xeste_id",4L,"xeste_adi_soyadi","Test Pasiyent","xeste_kodu","X0004","protokol_kodu","P0017","gelis_tarixi","2026-09-08","aciq_xidmet_sayi",2L,"xeste_umumi_meblegi",new BigDecimal("50.00"),"odenilen_mebleg",BigDecimal.ZERO,"borclandirilan_mebleg",BigDecimal.ZERO,"odenis_gozleyen_mebleg",new BigDecimal("50.00"))));
        var serviceRow=row("xeste_xidmet_id",101L,"xidmet_adi","Ümumi qan analizi","xidmet_kodu","LAB001","xidmet_tarixi","2026-09-08","status_adi","Açıq","secilib",true,"xeste_meblegi",new BigDecimal("50.00"),"odenilen_mebleg",BigDecimal.ZERO,"qalan_mebleg",new BigDecimal("50.00"),"paket_daxildir",false,"borclandirilib",false);
        when(repo.services(1L,7L,17L)).thenReturn(List.of(serviceRow));
        when(repo.paymentTypes()).thenReturn(List.of(Map.of("odenis_novu_id",1L,"ad","Nağd"),Map.of("odenis_novu_id",2L,"ad","Kart")));
        when(repo.receipts(1L,7L,17L)).thenReturn(List.of(row("emeliyyat_no","K26000001","emeliyyat_tarixi","2026-09-07 14:20","istiqamet","GIRIS","mebleg",new BigDecimal("10.00"),"muhasibat_kodu_adi","Xidmət gəliri","odenis_novleri","Nağd","aciqlama","Test","aktiv",true)));
        when(repo.debtors(1L,7L)).thenReturn(List.of(row("xeste_id",4L,"xeste_adi_soyadi","Test Pasiyent","xeste_kodu","X0004","aktiv_borclandirma_sayi",1L,"borclandirilan_umumi_mebleg",new BigDecimal("50.00"),"odenilen_mebleg",BigDecimal.TEN,"qalan_borc",new BigDecimal("40.00"))));
        when(repo.debtServices(1L,7L,4L)).thenReturn(List.of(row("borclandirma_xidmet_id",201L,"borclandirma_id",10L,"xidmet_adi","Ümumi qan analizi","xidmet_kodu","LAB001","borclandirma_tarixi","2026-09-07 14:20","protokol_kodu","P0017","borclandirilan_mebleg",new BigDecimal("50.00"),"odenilen_mebleg",BigDecimal.TEN,"qalan_borc",new BigDecimal("40.00"))));
    }
    @Test void homeRendersRealCashAndLocalizedCards() throws Exception {
        var result=mvc.perform(get("/kassa").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Mərkəzi Kassa"))).andExpect(content().string(containsString("Kassa Əməliyyat Mərkəzi"))).andReturn();
        preview("home",result.getResponse().getContentAsString());
    }
    @Test void paymentPageRendersServicesReceiptsAndCsrf() throws Exception {
        var result=mvc.perform(get("/kassa/odenis").servletPath("/kassa/odenis").param("kassaId","7").param("targetId","17").session(session))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Ümumi qan analizi")))
            .andExpect(content().string(containsString("K26000001"))).andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        preview("payment",result.getResponse().getContentAsString());
        String html=result.getResponse().getContentAsString();
        assertThat(html).contains("id=\"cash-payment-modal\"", "modal-dialog-scrollable", "data-bs-dismiss=\"modal\"");
        assertThat(html.indexOf("id=\"cash-payment-modal\"")).isLessThan(html.indexOf("id=\"cash-payment-form\""));
    }
    @Test void debtPageOpensPaymentModalWithDebtServiceId() throws Exception {
        var result=mvc.perform(get("/kassa/borc").servletPath("/kassa/borc").param("kassaId","7").param("targetId","4").session(session))
            .andExpect(status().isOk()).andExpect(content().string(containsString("value=\"201\""))).andReturn();
        preview("debt",result.getResponse().getContentAsString());
        String html=result.getResponse().getContentAsString();
        assertThat(html).contains("id=\"cash-payment-modal\"", "modal-dialog-scrollable", "Borc ödənişi", "data-debt=\"true\"");
        assertThat(html.indexOf("id=\"cash-payment-modal\"")).isLessThan(html.indexOf("id=\"cash-payment-form\""));
    }
    @Test void emptyCashListHasNoEnabledPaymentLinks() throws Exception {
        when(service.cashRegisters(any(),any())).thenReturn(List.of());
        mvc.perform(get("/kassa").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Kassa təyin edilməyib")));
    }
    @Test void replayedFormCannotSubmitPaymentTwice() throws Exception {
        var page=mvc.perform(get("/kassa/odenis").servletPath("/kassa/odenis").param("kassaId","7").param("targetId","17").session(session)).andExpect(status().isOk()).andReturn();
        var token=(String)page.getModelAndView().getModel().get("paymentToken");
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.pay(any(),eq(1L),eq(7L),eq(17L),eq(false),anyList(),anyList(),anyList(),eq(false),any()))
                .thenReturn(Map.of("ugurlu",true,"status_kodu","UGURLU"));
        for(int i=0;i<2;i++) {
            mvc.perform(post("/kassa/odenis").servletPath("/kassa/odenis").session(session).param(csrf.getParameterName(),csrf.getToken())
                    .param("kassaId","7").param("targetId","17").param("paymentToken",token).param("serviceIds","101").param("paymentTypeIds","1").param("amounts","50.00"))
                    .andExpect(status().is3xxRedirection());
        }
        verify(service,times(1)).pay(any(),eq(1L),eq(7L),eq(17L),eq(false),anyList(),anyList(),anyList(),eq(false),any());
        verifyNoInteractions(dataSource);
    }
    @Test void paymentWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/kassa/odenis").session(session).param("kassaId","7").param("targetId","17").param("paymentToken","invalid"))
                .andExpect(status().isForbidden());
        verify(service,never()).pay(any(),any(),any(),any(),anyBoolean(),any(),any(),any(),anyBoolean(),any());
    }
    private void preview(String name,String html) throws Exception {
        assertThat(html).doesNotContain("??kassa.");
        Path directory=Path.of("target/kassa-preview");Files.createDirectories(directory);Files.writeString(directory.resolve(name+".html"),html);
    }
}
