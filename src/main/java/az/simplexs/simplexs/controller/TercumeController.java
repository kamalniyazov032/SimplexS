package az.simplexs.simplexs.controller;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.dto.tercume.TercumeCedvelSetri;
import az.simplexs.simplexs.repository.tercume.TercumeRepository;

@Controller
public class TercumeController {
    private final TercumeRepository repo;
    public TercumeController(TercumeRepository repo){this.repo=repo;}
    @GetMapping(value="/tercumeler",params="bolme=melumat") public String melumatlar(@RequestParam(defaultValue="CINS")String melumatNovu,@RequestParam(required=false)String q,@RequestParam(defaultValue="false")boolean yalnizBos,Model m){
        var diller=repo.diller().stream().filter(x->Boolean.TRUE.equals(x.aktiv())).toList();var tercumeDilleri=diller.stream().filter(x->!Boolean.TRUE.equals(x.standartdir())).toList();
        var novler=repo.melumatNovleri();final String requested=melumatNovu;final String selected=novler.stream().anyMatch(x->x.kod().equals(requested))?requested:"CINS";
        String query=q==null?"":q.trim().toLowerCase(Locale.forLanguageTag("az"));var rows=repo.melumatSetirleri(selected).stream()
            .filter(x->query.isBlank()||x.kod().toLowerCase(Locale.ROOT).contains(query)||x.azerbaycanca().toLowerCase(Locale.forLanguageTag("az")).contains(query))
            .filter(x->!yalnizBos||tercumeDilleri.stream().anyMatch(d->x.deyer(d.kod()).isBlank())).toList();
        m.addAttribute("pageTitle","Məlumat tərcümələri");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","tercumeler");m.addAttribute("bolme","melumat");
        m.addAttribute("diller",diller);m.addAttribute("tercumeDilleri",tercumeDilleri);m.addAttribute("melumatNovleri",novler);m.addAttribute("selectedMelumatNovu",selected);m.addAttribute("melumatSetirleri",rows);m.addAttribute("q",q);m.addAttribute("yalnizBos",yalnizBos);return "pages/tercumeler";
    }
    @GetMapping("/tercumeler") public String list(@RequestParam(required=false)String modul,@RequestParam(required=false)String q,
            @RequestParam(defaultValue="false")boolean yalnizBos,@RequestParam(defaultValue="1")int page,Model m){
        var diller=repo.diller().stream().filter(x->Boolean.TRUE.equals(x.aktiv())).toList();
        var tercumeDilleri=diller.stream().filter(x->!Boolean.TRUE.equals(x.standartdir())).toList();
        var modullar=repo.modullar();String selected=modul==null||modul.isBlank()?"GLOBAL":modul;
        var selectedModulInfo=modullar.stream().filter(x->x.kod().equals(selected)).findFirst().orElse(null);
        var modulQruplari=new LinkedHashMap<String,List<az.simplexs.simplexs.dto.tercume.TercumeModulu>>();
        modullar.stream().filter(x->x.sistemKodu()!=null).forEach(x->modulQruplari.computeIfAbsent(x.sistemKodu(),k->new ArrayList<>()).add(x));
        var az=loadDefaults();var loadedKeys=repo.acarlar(selected);if(loadedKeys.isEmpty()&&modullar.isEmpty())loadedKeys=az.keySet().stream().sorted().toList();final var keys=loadedKeys;
        var byLanguage=new LinkedHashMap<String,Map<String,String>>();
        tercumeDilleri.forEach(d->byLanguage.put(d.kod(),repo.tercumeler(d.kod())));
        String query=q==null?"":q.trim().toLowerCase(Locale.forLanguageTag("az"));
        var allRows=keys.stream().filter(az::containsKey).map(key->{
                var values=new LinkedHashMap<String,String>();
                byLanguage.forEach((language,translations)->values.put(language,translations.getOrDefault(key,"")));
                return new TercumeCedvelSetri(key,az.get(key),values);
            })
            .filter(row->query.isBlank()||row.acar().toLowerCase(Locale.ROOT).contains(query)||row.azerbaycanca().toLowerCase(Locale.forLanguageTag("az")).contains(query)||row.deyerler().values().stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains(query)))
            .filter(row->!yalnizBos||tercumeDilleri.stream().anyMatch(d->row.deyer(d.kod()).isBlank())).toList();
        int size=50,totalPages=Math.max(1,(int)Math.ceil(allRows.size()/(double)size));page=Math.max(1,Math.min(page,totalPages));int from=(page-1)*size,to=Math.min(from+size,allRows.size());
        var completion=new LinkedHashMap<String,Integer>();int denominator=Math.max(1,keys.size());
        tercumeDilleri.forEach(d->{long count=keys.stream().filter(k->!byLanguage.get(d.kod()).getOrDefault(k,"").isBlank()).count();completion.put(d.kod(),(int)Math.round(count*100.0/denominator));});
        m.addAttribute("pageTitle","Tərcümələr");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","tercumeler");
        m.addAttribute("diller",diller);m.addAttribute("tercumeDilleri",tercumeDilleri);m.addAttribute("modullar",modullar);m.addAttribute("selectedModul",selected);
        m.addAttribute("selectedModulInfo",selectedModulInfo);m.addAttribute("umumiModul",modullar.stream().filter(x->x.kod().equals("GLOBAL")).findFirst().orElse(null));m.addAttribute("modulQruplari",modulQruplari);
        m.addAttribute("tercumeler",allRows.subList(from,to));m.addAttribute("completion",completion);m.addAttribute("q",q);m.addAttribute("yalnizBos",yalnizBos);
        m.addAttribute("currentPage",page);m.addAttribute("totalPages",totalPages);m.addAttribute("totalCount",allRows.size());return "pages/tercumeler";
    }
    @PostMapping("/tercumeler/dil") public String dil(@RequestParam String kod,@RequestParam String ad,@RequestParam String yerliAd,RedirectAttributes a){repo.dilYarat(kod,ad,yerliAd);a.addFlashAttribute("successMessage","Dil əlavə edildi.");return "redirect:/tercumeler";}
    @PostMapping("/tercumeler/yadda-saxla") public String save(@RequestParam String dil,@RequestParam String acar,@RequestParam String deyer,
            @RequestParam(defaultValue="GLOBAL")String modul,@RequestParam(required=false)String q,@RequestParam(defaultValue="false")boolean yalnizBos,
            @RequestParam(defaultValue="1")int page,RedirectAttributes a){repo.tercumeYaddaSaxla(dil,acar,deyer);a.addFlashAttribute("successMessage","Tərcümə yadda saxlanıldı.");a.addAttribute("modul",modul);a.addAttribute("q",q);a.addAttribute("yalnizBos",yalnizBos);a.addAttribute("page",page);return "redirect:/tercumeler";}
    @PostMapping(value="/tercumeler/yadda-saxla",params="bolme=melumat") public String melumatSave(@RequestParam String melumatNovu,@RequestParam Long menbeId,@RequestParam String saha,@RequestParam String dil,@RequestParam String deyer,@RequestParam(required=false)String q,@RequestParam(defaultValue="false")boolean yalnizBos,RedirectAttributes a){repo.melumatTercumesiYaddaSaxla(melumatNovu,menbeId,saha,dil,deyer);a.addFlashAttribute("successMessage","Məlumat tərcüməsi yadda saxlanıldı.");a.addAttribute("bolme","melumat");a.addAttribute("melumatNovu",melumatNovu);a.addAttribute("q",q);a.addAttribute("yalnizBos",yalnizBos);return "redirect:/tercumeler";}
    private Map<String,String> loadDefaults(){var b=ResourceBundle.getBundle("messages",Locale.forLanguageTag("az"));var result=new LinkedHashMap<String,String>();b.keySet().forEach(k->result.put(k,b.getString(k)));return result;}
}

@ControllerAdvice
class DilAdvice {
    private final TercumeRepository repo;
    DilAdvice(TercumeRepository repo){this.repo=repo;}
    @ModelAttribute void diller(Model m){m.addAttribute("aktivDiller",repo.diller().stream().filter(x->Boolean.TRUE.equals(x.aktiv())).toList());}
}
