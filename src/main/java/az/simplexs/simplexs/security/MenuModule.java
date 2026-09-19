package az.simplexs.simplexs.security;

import java.util.ArrayList;
import java.util.List;

public class MenuModule {
    private final Long id;
    private final Long parentId;
    private final String code;
    private final String name;
    private final String route;
    private final String icon;
    private final List<MenuModule> children = new ArrayList<>();

    public MenuModule(Long id, Long parentId, String code, String name, String route, String icon) {
        this.id=id; this.parentId=parentId; this.code=code; this.name=name; this.route=route; this.icon=icon;
    }
    public Long getId(){return id;} public Long getParentId(){return parentId;} public String getCode(){return code;}
    public String getName(){return name;} public String getRoute(){
        if(route != null)return route;
        if(isInvoiceModule())return "/eczaxana/qaimeler";
        if("HIS_PHARMACY_REQUESTS_SENT".equals(code))return "/eczaxana/telebler";
        if("HIS_PHARMACY_REQUESTS_INCOMING".equals(code))return "/eczaxana/telebler/qarsilama";
        return null;
    }
    public boolean isInvoiceModule(){return "HIS_PHARMACY_INVOICES".equals(code);} public String getIcon(){return icon;}
    public List<MenuModule> getChildren(){return children;}

    public boolean containsRoute(String requestUri) {
        if (requestUri == null) return false;
        String destination = getRoute();
        if (destination != null && (requestUri.equals(destination) || requestUri.startsWith(destination + "/"))) return true;
        return children.stream().anyMatch(child -> child.containsRoute(requestUri));
    }
}
