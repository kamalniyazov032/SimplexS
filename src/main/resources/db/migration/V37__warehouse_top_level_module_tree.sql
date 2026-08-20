-- rn_ menyu və rol metadata-sına toxunur; canlı DB-yə ayrıca açıq razılıqla tətbiq edilməlidir.
-- Anbarı Parametrlərdən ayırıb eyni İnzibati idarəetmə sistemi daxilində əsas menyu qrupu edir.
UPDATE public.rn_modullar warehouse
SET parent_id=NULL,route=NULL,ad='Anbar',aciqlama='Anbar məlumatlarının idarə edilməsi',
    ikon='ti ti-building-warehouse',menyuda_gorunsun=true,aktiv=true,sira_no=45
WHERE warehouse.kod='APP_WAREHOUSE_PARAMS';

WITH warehouse AS (
    SELECT id,sistem_id FROM public.rn_modullar WHERE kod='APP_WAREHOUSE_PARAMS'
), items(kod,ad,aciqlama,route,ikon,sira_no) AS (VALUES
 ('APP_WAREHOUSE_LIST','Anbarlar','Anbarların yaradılması və idarə edilməsi','/anbar/anbarlar','ti ti-building-warehouse',10),
 ('APP_WAREHOUSE_COMPANIES','Firmalar','Təchizatçı firmaların idarə edilməsi','/anbar/firmalar','ti ti-building-factory-2',20),
 ('APP_WAREHOUSE_UNITS','Vahidlər','Ölçü və qablaşdırma vahidlərinin idarə edilməsi','/anbar/vahidler','ti ti-ruler-measure',30),
 ('APP_WAREHOUSE_GROUPS','Məhsul qrupları','Məhsul qruplarının idarə edilməsi','/anbar/qruplar','ti ti-category-2',40),
 ('APP_WAREHOUSE_MATERIALS','Vəsaitlər','Vəsait və materialların idarə edilməsi','/anbar/materiallar','ti ti-pill',50),
 ('APP_WAREHOUSE_OPERATIONS','Əməliyyat növləri','Anbar əməliyyat növlərinin idarə edilməsi','/anbar/emeliyyatlar','ti ti-arrows-exchange',60)
)
INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT w.sistem_id,w.id,i.kod,i.ad,i.aciqlama,i.route,i.ikon,true,true,i.sira_no FROM warehouse w CROSS JOIN items i
ON CONFLICT(kod) DO UPDATE SET sistem_id=EXCLUDED.sistem_id,parent_id=EXCLUDED.parent_id,ad=EXCLUDED.ad,
 aciqlama=EXCLUDED.aciqlama,route=EXCLUDED.route,ikon=EXCLUDED.ikon,menyuda_gorunsun=true,aktiv=true,sira_no=EXCLUDED.sira_no;

-- Köhnə Anbar moduluna çıxışı olan rollara yeni alt modulları da verir.
INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT parent_access.rol_id,child.id,true
FROM public.rn_rol_modullari parent_access
JOIN public.rn_modullar parent ON parent.id=parent_access.modul_id AND parent.kod='APP_WAREHOUSE_PARAMS'
JOIN public.rn_modullar child ON child.parent_id=parent.id AND child.aktiv
WHERE parent_access.aktiv
ON CONFLICT(rol_id,modul_id) DO UPDATE SET aktiv=true;
