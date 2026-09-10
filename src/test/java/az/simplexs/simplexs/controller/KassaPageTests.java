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
    @MockitoBean az.simplexs.simplexs.repository.xeste.XesteRepository patients;
    @MockitoBean az.simplexs.simplexs.repository.teskilat.TeskilatRepository patientOrganizations;
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
    @Test void otherIncomePopupRendersAccountingCodesAndPaymentTypes() throws Exception {
        when(repo.accountingCodes(1L,"DIGER_GIRIS")).thenReturn(List.of(row("muhasibat_kodu_id",5L,"ad","Digər gəlirlər","aciqlama","Test açıqlama")));
        var page=mvc.perform(get("/kassa/diger-giris").param("kassaId","7").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"other-income-modal\"")))
                .andExpect(content().string(containsString("Digər gəlirlər"))).andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        preview("other-income",page.getResponse().getContentAsString());
        verify(repo).accountingCodes(1L,"DIGER_GIRIS");
        verifyNoInteractions(dataSource);
    }
    @Test void otherIncomeSubmissionIsScopedAndCannotBeReplayed() throws Exception {
        var page=mvc.perform(get("/kassa/diger-giris").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        var token=(String)page.getModelAndView().getModel().get("incomeToken");
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.otherIncome(any(),eq(1L),eq(7L),eq(5L),anyList(),anyList(),any()))
                .thenReturn(Map.of("ugurlu",true,"status_kodu","UGURLU"));
        for(int i=0;i<2;i++) {
            mvc.perform(post("/kassa/diger-giris").session(session).param(csrf.getParameterName(),csrf.getToken())
                    .param("kassaId","7").param("paymentToken",token).param("accountId","5").param("paymentTypeIds","1").param("amounts","250.00"))
                    .andExpect(status().is3xxRedirection());
        }
        verify(service,times(1)).otherIncome(any(),eq(1L),eq(7L),eq(5L),eq(List.of(1L)),eq(List.of(new BigDecimal("250.00"))),isNull());
        verifyNoInteractions(dataSource);
    }
    @Test void otherIncomeNeedsCsrfAndWriteAccess() throws Exception {
        mvc.perform(post("/kassa/diger-giris").session(session).param("kassaId","7").param("paymentToken","invalid"))
                .andExpect(status().isForbidden());
        when(service.requireCash(any(),eq(1L),eq(7L),eq(true))).thenThrow(new org.springframework.security.access.AccessDeniedException("denied"));
        mvc.perform(get("/kassa/diger-giris").param("kassaId","7").session(session)).andExpect(status().isForbidden());
        verify(service,never()).otherIncome(any(),any(),any(),any(),any(),any(),any());
    }
    @Test void failedOtherIncomeRetainsDraftAndReopensPopup() throws Exception {
        var page=mvc.perform(get("/kassa/diger-giris").param("kassaId","7").session(session)).andReturn();
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.otherIncome(any(),any(),any(),any(),any(),any(),any()))
                .thenReturn(Map.of("ugurlu",false,"status_kodu","UNKNOWN_CODE","mesaj","Internal DB details"));
        var result=mvc.perform(post("/kassa/diger-giris").session(session).param(csrf.getParameterName(),csrf.getToken())
                .param("kassaId","7").param("paymentToken",(String)page.getModelAndView().getModel().get("incomeToken"))
                .param("accountId","5").param("paymentTypeIds","1").param("amounts","250.00").param("note","Test note"))
                .andExpect(redirectedUrl("/kassa/diger-giris?kassaId=7")).andExpect(flash().attribute("incomeNote","Test note")).andReturn();
        assertThat(result.getFlashMap().get("errorMessage").toString()).doesNotContain("Internal DB details");
        when(repo.accountingCodes(1L,"DIGER_GIRIS")).thenReturn(List.of(row("muhasibat_kodu_id",5L,"ad","Digər gəlirlər","aciqlama","Test")));
        mvc.perform(get("/kassa/diger-giris").session(session).param("kassaId","7").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("value=\"250.00\"")));
    }
    private Map<String,Object> advancePatient() {
        return row("gelis_id",17L,"xeste_id",4L,"l_kart","X0004","gelis_karti","AMB0017","kart_novu_adi","Ambulator",
                "ad","Kamal","soyad","Niyazov","ata_adi","Əli","dogum_tarixi","1985-03-12","gelis_tarixi","2026-09-09");
    }
    @Test void advancePopupRendersSearchWithoutAutomaticallyLoadingPatients() throws Exception {
        when(repo.cardTypes()).thenReturn(List.of(Map.of("kod","AMBULATOR","ad","Ambulator")));
        var page=mvc.perform(get("/kassa/avans").param("kassaId","7").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"advance-query\"")))
                .andExpect(content().string(containsString("data-advance=\"true\""))).andReturn();
        preview("advance",page.getResponse().getContentAsString());
        verify(repo).accountingCodes(1L,"AVANS_QEBUL");
        verify(repo,never()).advancePatients(any(),any(),any(),any(),any(),any(),anyInt());
        verifyNoInteractions(dataSource);
    }
    @Test void advanceSearchShowsIdentityAndDatesAndEscapesPatientData() throws Exception {
        var patient=advancePatient();patient.put("ata_adi","<script>bad</script>");
        when(repo.advancePatients(1L,7L,null,"Kamal Niyazov",null,null,1)).thenReturn(List.of(patient));
        mvc.perform(get("/kassa/avans/xesteler").param("kassaId","7").param("q","Kamal Niyazov").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("AMB0017")))
                .andExpect(content().string(containsString("1985-03-12"))).andExpect(content().string(containsString("2026-09-09")))
                .andExpect(content().string(containsString("&lt;script&gt;bad&lt;/script&gt;")));
        verifyNoInteractions(dataSource);
    }
    @Test void advanceSearchValidatesDatesAndScopesCardFilters() throws Exception {
        when(repo.cardTypes()).thenReturn(List.of(Map.of("kod","AMBULATOR")));
        mvc.perform(get("/kassa/avans/xesteler").param("kassaId","7").param("q","AMB0017").param("cardType","AMBULATOR")
                .param("from","2026-09-01").param("to","2026-09-09").session(session)).andExpect(status().isOk());
        verify(repo).advancePatients(1L,7L,"AMBULATOR","AMB0017",java.time.LocalDate.of(2026,9,1),java.time.LocalDate.of(2026,9,9),1);
        clearInvocations(repo);
        mvc.perform(get("/kassa/avans/xesteler").param("kassaId","7").param("from","2026-09-10").param("to","2026-09-01").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("tarix aralığını yoxlayın")));
        verify(repo,never()).advancePatients(any(),any(),any(),any(),any(),any(),anyInt());
    }
    @Test void advanceUsesItsOwnTokenAndIgnoresClientPatientId() throws Exception {
        var page=mvc.perform(get("/kassa/avans").param("kassaId","7").session(session)).andReturn();
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.advance(any(),eq(1L),eq(7L),eq(17L),eq(3L),anyList(),anyList(),any()))
                .thenReturn(Map.of("ugurlu",true,"status_kodu","UGURLU"));
        var token=(String)page.getModelAndView().getModel().get("incomeToken");
        for(int i=0;i<2;i++) mvc.perform(post("/kassa/avans").session(session).param(csrf.getParameterName(),csrf.getToken())
                .param("kassaId","7").param("paymentToken",token).param("visitId","17").param("patientId","999")
                .param("accountId","3").param("paymentTypeIds","1").param("amounts","100.00"))
                .andExpect(status().is3xxRedirection());
        verify(service,times(1)).advance(any(),eq(1L),eq(7L),eq(17L),eq(3L),eq(List.of(1L)),eq(List.of(new BigDecimal("100.00"))),isNull());
    }
    @Test void advanceFailureRestoresSelectedPatientAndAmount() throws Exception {
        when(repo.advanceVisit(1L,7L,17L)).thenReturn(advancePatient());
        mvc.perform(get("/kassa/avans").param("kassaId","7").session(session)
                .flashAttr("advanceVisitId",17L).flashAttr("incomeAmounts",Map.of(1L,new BigDecimal("100.00"))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Kamal Niyazov Əli")))
                .andExpect(content().string(containsString("value=\"100.00\"")));
    }
    @Test void sixHundredAdvanceResultsAreDisplayedInSixPagesOfOneHundred() throws Exception {
        for(int page=1;page<=6;page++) {
            List<Map<String,Object>> rows=new ArrayList<>();
            int fetched=page<6?101:100;
            for(int i=0;i<fetched;i++) {
                var patient=new LinkedHashMap<>(advancePatient());
                patient.put("gelis_id",(long)(page-1)*100+i+1);
                rows.add(patient);
            }
            when(repo.advancePatients(1L,7L,null,"Kamal",null,null,page)).thenReturn(rows);
            var result=mvc.perform(get("/kassa/avans/xesteler").session(session).param("kassaId","7")
                    .param("q","Kamal").param("page",String.valueOf(page))).andExpect(status().isOk()).andReturn();
            String html=result.getResponse().getContentAsString();
            assertThat(html.split("data-select-advance",-1).length-1).isEqualTo(100);
            assertThat(html).contains("Səhifə "+page+" · 100 nəticə");
            assertThat(result.getModelAndView().getModel().get("advanceHasMore")).isEqualTo(page<6);
            if(page==6) assertThat(html).contains("data-advance-page=\"7\" disabled=\"disabled\"");
        }
    }
    @Test void patientRegistrationErrorRendersEnteredValuesWithoutRedirect() throws Exception {
        when(patients.yarat(eq(1L), any(), eq(24L))).thenReturn(Map.of("status_kodu", "DUPLICATE_FIN", "mesaj", "Duplicate FIN"));
        var page = mvc.perform(get("/xeste-qeydiyyati/yeni").session(session)).andExpect(status().isOk()).andReturn();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/xeste-qeydiyyati/yeni").session(session).param(csrf.getParameterName(), csrf.getToken())
                .param("ad", "Entered name").param("soyad", "Entered surname").param("finKodu", "ABC1234")
                .param("vesiqeNomresi", "AA123").param("vesiqeNovuId", "2").param("cinsId", "1")
                .param("defaultTeskilatId", "3").param("dogumTarixi", "1990-01-01")
                .param("qeyd", "Keep my note").param("returnTo", "ambulator"))
                .andExpect(status().isOk()).andExpect(view().name("pages/pasienQebulu/xesteFormu"))
                .andExpect(content().string(containsString("Duplicate FIN")))
                .andExpect(content().string(containsString("value=\"Entered name\"")))
                .andExpect(content().string(containsString("value=\"ABC1234\"")))
                .andExpect(content().string(containsString("Keep my note")))
                .andExpect(content().string(containsString("value=\"ambulator\"")));
    }

    private void preview(String name,String html) throws Exception {
        assertThat(html).doesNotContain("??kassa.");
        Path directory=Path.of("target/kassa-preview");Files.createDirectories(directory);Files.writeString(directory.resolve(name+".html"),html);
    }
    @Test void refundOpensLocalizedServicePopup() throws Exception {
        when(repo.refundServices(1L,7L,17L)).thenReturn(List.of(row("gelis_id",17L,"xeste_id",4L,"xeste_kodu","X004","xeste_adi_soyadi","Test Pasiyent","protokol_kodu","A26000017","xeste_xidmet_id",101L,"xidmet_kodu","LAB01","xidmet_adi","Analiz","xidmet_tarixi","2026-09-09","status_kodu","YENI","status_adi","Yeni","secilib",true,"odenilen_mebleg",new BigDecimal("100.00"),"qaytarilan_mebleg",new BigDecimal("20.00"),"qaytarila_bilen_mebleg",new BigDecimal("80.00"))));
        when(repo.accountingCodes(1L,"XESTE_QAYTARMA")).thenReturn(List.of(row("muhasibat_kodu_id",7L,"ad","Qaytarma")));
        var result=mvc.perform(get("/kassa/qaytarma").servletPath("/kassa/qaytarma").param("kassaId","7").param("targetId","17").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("id=\"cash-payment-modal\"","data-refund=\"true\"","A26000017","Qaytarılacaq məbləğ","name=\"_csrf\"").doesNotContain("??kassa.");
        preview("refund",result.getResponse().getContentAsString());
    }
    @Test void expensePopupUsesExpenseActionAndAccountingCodes() throws Exception {
        when(repo.accountingCodes(1L,"DIGER_CIXIS")).thenReturn(List.of(row("muhasibat_kodu_id",18L,"ad","Təsərrüfat xərci","aciqlama","Test")));
        var page=mvc.perform(get("/kassa/diger-cixis").servletPath("/kassa/diger-cixis").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("action=\"/kassa/diger-cixis\"","data-expense=\"true\"","Çıxışı təsdiqləyin","Təsərrüfat xərci").doesNotContain("??kassa.");
        var token=(String)page.getModelAndView().getModel().get("incomeToken");
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.otherExpense(any(),eq(1L),eq(7L),eq(18L),anyList(),anyList(),any())).thenReturn(Map.of("ugurlu",true,"status_kodu","OK"));
        for(int i=0;i<2;i++) mvc.perform(post("/kassa/diger-cixis").servletPath("/kassa/diger-cixis").session(session).param(csrf.getParameterName(),csrf.getToken()).param("kassaId","7").param("paymentToken",token).param("accountId","18").param("paymentTypeIds","1").param("amounts","40.00")).andExpect(status().is3xxRedirection());
        verify(service,times(1)).otherExpense(any(),eq(1L),eq(7L),eq(18L),eq(List.of(1L)),eq(List.of(new BigDecimal("40.00"))),isNull());
    }
    @Test void balanceShowsCurrentTotalsWithReadAccess() throws Exception {
        when(repo.balance(1L,7L)).thenReturn(row("umumi_giris",new BigDecimal("150.00"),"umumi_cixis",new BigDecimal("175.00"),"cari_balans",new BigDecimal("-25.00")));
        var page=mvc.perform(get("/kassa/balans").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("Cari balans","-25","kassa-balance-modal").doesNotContain("??kassa.");
        verify(service,atLeastOnce()).requireCash(any(),eq(1L),eq(7L),eq(false));
    }
    @Test void datedBalanceDefaultsToTodayAndAcceptsExplicitRange() throws Exception {
        when(repo.balanceByDate(eq(1L),eq(7L),any(),any())).thenReturn(row("acilis_balansi",BigDecimal.TEN,"dovr_giris",BigDecimal.ZERO,"dovr_cixis",BigDecimal.ZERO,"baglanis_balansi",BigDecimal.TEN));
        mvc.perform(get("/kassa/balans-tarix").param("kassaId","7").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Açılış balansı")));
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku"));
        verify(repo).balanceByDate(1L,7L,today,today);
        mvc.perform(get("/kassa/balans-tarix").param("kassaId","7").param("from","2026-09-01").param("to","2026-09-09").session(session)).andExpect(status().isOk());
        verify(repo).balanceByDate(1L,7L,java.time.LocalDate.of(2026,9,1),java.time.LocalDate.of(2026,9,9));
    }
    @Test void invalidBalanceDatesDoNotQueryDatabase() throws Exception {
        for(String start:List.of("2026-09-10","not-a-date"))
            mvc.perform(get("/kassa/balans-tarix").param("kassaId","7").param("from",start).param("to","2026-09-09").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Düzgün tarix aralığı seçin")));
        verify(repo,never()).balanceByDate(any(),any(),any(),any());
    }
    @Test void balanceDeniesUnauthorizedCashBeforeQuery() throws Exception {
        when(service.requireCash(any(),eq(1L),eq(7L),eq(false))).thenThrow(new org.springframework.security.access.AccessDeniedException("Denied"));
        mvc.perform(get("/kassa/balans").param("kassaId","7").session(session)).andExpect(status().isForbidden());
        verify(repo,never()).balance(any(),any());
    }
    @Test void transferPopupAndReplayProtection() throws Exception {
        when(repo.balance(1L,7L)).thenReturn(row("cari_balans",new BigDecimal("75.00")));
        when(repo.transferRecipients(1L,7L)).thenReturn(List.of(row("kassa_id",2L,"kassa_kodu","K02","kassa_adi","Əsas kassa")));
        when(repo.accountingCodes(1L,"KASSA_TRANSFER")).thenReturn(List.of(row("muhasibat_kodu_id",8L,"ad","Transfer")));
        var page=mvc.perform(get("/kassa/transfer").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("max=\"75.00\"","Cari balans","Əsas kassa","Transferi təsdiqləyin","name=\"_csrf\"").doesNotContain("??kassa.");
        var token=(String)page.getModelAndView().getModel().get("transferToken");
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.transfer(any(),eq(1L),eq(7L),eq(2L),eq(8L),any(),any())).thenReturn(Map.of("ugurlu",true));
        for(int i=0;i<2;i++) mvc.perform(post("/kassa/transfer").session(session).param(csrf.getParameterName(),csrf.getToken()).param("kassaId","7").param("paymentToken",token).param("recipientId","2").param("accountId","8").param("amount","50.00")).andExpect(status().is3xxRedirection());
        verify(service,times(1)).transfer(any(),eq(1L),eq(7L),eq(2L),eq(8L),eq(new BigDecimal("50.00")),isNull());
    }
    private Map<String,Object> receiptRow() {
        var r=row("kassa_emeliyyat_id",10L,"emeliyyat_no","K2600010","emeliyyat_tarixi",java.sql.Timestamp.valueOf("2026-09-09 18:55:34.123456"),"istiqamet","GIRIS","mebleg",new BigDecimal("50.00"),"emeliyyat_kodu","DIGER_GIRIS","muhasibat_kodu_adi","Test hesab","xeste_adi_soyadi","Test Pasiyent","xeste_kodu","X004","protokol_kodu","A17","odenis_novleri","Nağd","yaradan_personal_adi","Kassir","aciqlama","<script>alert(1)</script>","aktiv",true,"transfer_id",null);
        r.put("status","TAMAMLANIB");
        r.put("odenisler","[{\"odenis_novu_adi\":\"Nağd\",\"mebleg\":50.00}]");
        r.put("xidmetler","[{\"xidmet_kodu\":\"LAB01\",\"xidmet_adi\":\"Analiz\",\"mebleg\":50.00}]");
        return r;
    }
    private void receiptSetup() {
        when(repo.receiptDetail(1L,7L,10L)).thenReturn(receiptRow());
        var paper=new LinkedHashMap<>(receiptRow());paper.put("status","TAMAMLANIB");paper.put("legv_sebebi",null);
        paper.put("klinika_adi","Test klinika");paper.put("kassa_adi","Mərkəzi Kassa");paper.put("kassir_adi","Kassir");
        paper.put("odenisler","[{\"ad\":\"Nağd\",\"mebleg\":50.00}]");
        when(repo.receiptPrint(1L,7L,10L)).thenReturn(paper);
        when(service.canCancelReceipt(any(),eq(1L))).thenReturn(true);
        when(service.receiptCancelReasons()).thenReturn(List.of(new az.simplexs.simplexs.dto.sebeb.Sebeb(9L,5L,"QEBZ_LEGV","","TEST","Test səbəbi",null,1,true)));
    }
    @Test void receiptListRendersOffcanvasAndLimitsRows() throws Exception {
        when(repo.receiptTotals(eq(1L),eq(7L),eq(""),any(),any(),eq(""),eq(""),eq("")))
                .thenReturn(row("income",new BigDecimal("6000.00"),"expense",new BigDecimal("750.00"),"net",new BigDecimal("5250.00")));

        when(repo.receiptFilterOptions(1L,7L)).thenReturn(List.of(
                row("kind","status","code","TAMAMLANIB"), row("kind","status","code","LEGV_EDILIB"),
                row("kind","operation","code","DIGER_GIRIS")));

        when(repo.allReceipts(eq(1L),eq(7L),eq(""),eq(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku"))),eq(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku"))),eq(""),eq(""),eq(""),eq(1))).thenReturn(Collections.nCopies(101,receiptRow()));
        var page=mvc.perform(get("/kassa/qebzler").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        assertThat((List<?>)page.getModelAndView().getModel().get("receipts")).hasSize(100);
        assertThat(page.getModelAndView().getModel().get("from")).isEqualTo(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku")).toString());
        assertThat(page.getModelAndView().getModel().get("to")).isEqualTo(page.getModelAndView().getModel().get("from"));
        assertThat(page.getModelAndView().getModel().get("hasMore")).isEqualTo(true);
        assertThat(page.getResponse().getContentAsString()).contains("<tfoot", "5250,00", "A17");
        assertThat(page.getResponse().getContentAsString()).contains("receipt-cancel-action","cancel=true","offcanvas-top_ka","K2600010","09.09.2026 18:55","&lt;script&gt;").contains("receipt-status", "LEGV_EDILIB", "TAMAMLANIB", "name=\"status\"").doesNotContain("receipt-active", "<script>alert(1)</script>","??kassa.");
    }
    @Test void receiptDetailPrintAndCancellationAreScopedAndReplaySafe() throws Exception {
        receiptSetup();
        var page=mvc.perform(get("/kassa/qebzler/10").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("Analiz","Nağd","receipt-cancel-form").doesNotContain("??kassa.");
        mvc.perform(get("/kassa/qebzler/10/cap").param("kassaId","7").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Test klinika")));
        var token=(String)page.getModelAndView().getModel().get("cancelToken");
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.cancelReceipt(any(),eq(1L),eq(7L),eq(10L),eq(9L))).thenReturn(Map.of("ugurlu",true));
        for(int i=0;i<2;i++) mvc.perform(post("/kassa/qebzler/10/legv").session(session).param(csrf.getParameterName(),csrf.getToken()).param("kassaId","7").param("paymentToken",token).param("reasonId","9")).andExpect(status().is3xxRedirection());
        verify(service,times(1)).cancelReceipt(any(),eq(1L),eq(7L),eq(10L),eq(9L));
    }
    @Test void receiptCancellationHiddenWithoutPermissionAndMissingReceiptIs404() throws Exception {
        receiptSetup();when(service.canCancelReceipt(any(),eq(1L))).thenReturn(false);
        var page=mvc.perform(get("/kassa/qebzler/10").param("kassaId","7").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).doesNotContain("id=\"receipt-cancel-form\"");
        mvc.perform(get("/kassa/qebzler/99").param("kassaId","7").session(session)).andExpect(status().isNotFound());
    }
    @Test void advanceRefundPopupAndSubmissionReplayProtection() throws Exception {
        when(repo.advanceRefundPatients(1L,7L,null,"",null,null,1)).thenReturn(List.of(advancePatient()));
        when(repo.refundableAdvances(1L,7L,17L)).thenReturn(List.of(row("xeste_id",4L,"xeste_kodu","X004","xeste_adi_soyadi","Test Pasiyent","gelis_id",17L,"protokol_kodu","A17","avans_id",1L,"avans_no","AV001","ilk_mebleg",new BigDecimal("100.00"),"istifade_edilen_mebleg",new BigDecimal("50.00"),"qaytarilan_mebleg",new BigDecimal("25.00"),"qalan_mebleg",new BigDecimal("25.00"),"status","AKTIV","avans_tarixi","2026-09-09")));
        when(repo.accountingCodes(1L,"AVANS_QAYTARMA")).thenReturn(List.of(row("muhasibat_kodu_id",17L,"ad","Avans qaytarma")));
        var page=mvc.perform(get("/kassa/avans-qaytarma").param("kassaId","7").param("targetId","17").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("AV001","Avans qalığı","advance-refund-modal","data-balance=\"25.00\"").doesNotContain("??kassa.");
        var token=(String)page.getModelAndView().getModel().get("paymentToken");
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        when(service.refundAdvance(any(),eq(1L),eq(7L),eq(17L),eq(1L),eq(17L),eq(1L),any(),any())).thenReturn(Map.of("ugurlu",true));
        for(int i=0;i<2;i++) mvc.perform(post("/kassa/avans-qaytarma").session(session).param(csrf.getParameterName(),csrf.getToken()).param("kassaId","7").param("targetId","17").param("paymentToken",token).param("advanceId","1").param("accountId","17").param("typeId","1").param("amount","20.00")).andExpect(status().is3xxRedirection());
        verify(service,times(1)).refundAdvance(any(),eq(1L),eq(7L),eq(17L),eq(1L),eq(17L),eq(1L),eq(new BigDecimal("20.00")),isNull());
    }
    @Test void receiptsOpenDetailsInModalAndFilterByStatus() throws Exception {
        receiptSetup();
        var page=mvc.perform(get("/kassa/qebzler").param("kassaId","7").param("receiptId","10").param("cancel","true").session(session)).andExpect(status().isOk()).andReturn();
        assertThat(page.getModelAndView().getModel().get("receiptStatus")).isEqualTo("");
        assertThat(page.getResponse().getContentAsString()).contains("id=\"receipt-detail-modal\"","data-cancel=\"true\"","receipt-cancel-form").doesNotContain("??kassa.");
        mvc.perform(get("/kassa/qebzler").param("kassaId","7").param("status","LEGV_EDILIB").session(session)).andExpect(status().isOk());
        verify(repo).allReceipts(eq(1L),eq(7L),eq(""),any(),any(),eq(""),eq(""),eq("LEGV_EDILIB"),eq(1));
    }
}
