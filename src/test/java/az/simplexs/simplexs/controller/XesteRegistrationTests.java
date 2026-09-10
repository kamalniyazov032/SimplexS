package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.dto.xeste.XesteForm;
import az.simplexs.simplexs.repository.xeste.XesteRepository;
import az.simplexs.simplexs.repository.teskilat.TeskilatRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class XesteRegistrationTests {
    XesteRepository repo = mock(XesteRepository.class);
    TeskilatRepository organizations = mock(TeskilatRepository.class);
    StaticMessageSource messages = new StaticMessageSource();
    XesteQeydiyyatiController controller;
    MockHttpSession session = new MockHttpSession();
    AuthenticatedPersonal personal = new AuthenticatedPersonal(24L,"cashier","","Test",true,List.of());
    ExtendedModelMap model = new ExtendedModelMap();
    RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
    XesteForm form = new XesteForm();
    BeanPropertyBindingResult binding;
    @BeforeEach void setup() {
        messages.setUseCodeAsDefaultMessage(true);
        controller = new XesteQeydiyyatiController(repo, organizations, messages);
        session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID, 1L);
        form.ad="Test"; form.soyad="Patient"; form.finKodu="ABC1234"; form.vesiqeNomresi="AA123";
        form.cinsId=1L; form.vesiqeNovuId=2L; form.defaultTeskilatId=3L;
        form.dogumTarixi=LocalDate.of(1990,1,1); form.qeyd="Keep this note"; form.seherId=9L;
        binding = new BeanPropertyBindingResult(form,"xesteForm");
    }
    String create(String returnTo) { return controller.yarat(form,binding,returnTo,session,personal,flash,model); }
    @Test void duplicateFinKeepsEverySubmittedFieldAndReturnDestination() {
        when(repo.yarat(1L,form,24L)).thenReturn(Map.of("status_kodu","DUPLICATE_FIN","mesaj","Duplicate FIN"));
        assertThat(create("ambulator")).isEqualTo("pages/pasienQebulu/xesteFormu");
        assertThat(model.get("submittedForm")).isSameAs(form);
        assertThat(model.get("registrationError")).isEqualTo("Duplicate FIN");
        assertThat(model.get("returnTo")).isEqualTo("ambulator");
        assertThat(model.get("editing")).isEqualTo(false);
        assertThat(flash.getFlashAttributes()).isEmpty();
    }
    @Test void onlySuccessfulCreationRedirects() {
        when(repo.yarat(1L,form,24L)).thenReturn(Map.of("status_kodu","1","xeste_id",17L));
        assertThat(create(null)).isEqualTo("redirect:/xeste-qeydiyyati/siyahi");
        assertThat(create("ambulator")).isEqualTo("redirect:/ambulatorQebul/yeni?xesteId=17");
    }
    @Test void missingRequiredDataDoesNotAttemptSaving() {
        form.ad=" ";
        assertThat(create(null)).isEqualTo("pages/pasienQebulu/xesteFormu");
        verify(repo,never()).yarat(any(),any(),any());
        assertThat(model.get("submittedForm")).isSameAs(form);
    }
    @Test void bindingErrorsKeepTheFormWithoutSaving() {
        binding.rejectValue("dogumTarixi","typeMismatch");
        assertThat(create(null)).isEqualTo("pages/pasienQebulu/xesteFormu");
        verify(repo,never()).yarat(any(),any(),any());
    }
    @Test void failedEditKeepsEditedValuesAndEditMode() {
        form.xesteId=17L; form.aktiv=false;
        when(repo.yenile(1L,form,24L)).thenReturn(Map.of("status_kodu","ERROR","mesaj","Duplicate document"));
        assertThat(controller.yenile(form,binding,session,personal,flash,model)).isEqualTo("pages/pasienQebulu/xesteFormu");
        assertThat(model.get("submittedForm")).isSameAs(form);
        assertThat(model.get("editing")).isEqualTo(true);
    }
    @Test void databaseExceptionKeepsFormAndUsesLocalizedMessage() {
        when(repo.yarat(1L,form,24L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private database details"));
        assertThat(create(null)).isEqualTo("pages/pasienQebulu/xesteFormu");
        assertThat(model.get("submittedForm")).isSameAs(form);
        assertThat(model.get("registrationError")).isEqualTo("patients.save_error");
    }
}
