package az.simplexs.simplexs.security;

import java.util.ArrayList;
import java.util.List;

public class MenuSystem {
    private final Long id;
    private final String code;
    private final String name;
    private final String icon;
    private final List<MenuModule> modules = new ArrayList<>();

    public MenuSystem(Long id, String code, String name, String icon) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.icon = icon;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public List<MenuModule> getModules() { return modules; }
}
