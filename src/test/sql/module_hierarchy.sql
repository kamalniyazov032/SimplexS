-- Run only in an isolated test database after installing V47 and the minimal rn_ fixture.
DO $$
DECLARE r record; root_id bigint; child_id bigint; leaf_id bigint; other_id bigint;
BEGIN
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,NULL,'ROOT','Root',NULL,NULL,1);
    ASSERT r.status_kodu='UGURLU'; root_id:=r.modul_id;
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,root_id,'CHILD','Child',NULL,NULL,1);
    ASSERT r.status_kodu='UGURLU'; child_id:=r.modul_id;
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,child_id,'LEAF','Leaf',NULL,NULL,1);
    ASSERT r.status_kodu='UGURLU'; leaf_id:=r.modul_id;
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,leaf_id,'TOO_DEEP','Too deep',NULL,NULL,1);
    ASSERT r.mesaj='modules.depth_exceeded';
    SELECT * INTO r FROM public.kn_modul_yenile(root_id,1,leaf_id,'Root',NULL,NULL,1,true,true);
    ASSERT r.mesaj='modules.cycle';
    SELECT * INTO r FROM public.kn_modul_yenile(child_id,1,child_id,'Child',NULL,NULL,1,true,true);
    ASSERT r.mesaj='modules.cycle';
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,NULL,'OTHER','Other',NULL,NULL,1);
    other_id:=r.modul_id;
    SELECT * INTO r FROM public.kn_modul_yenile(root_id,1,other_id,'Root',NULL,NULL,1,true,true);
    ASSERT r.mesaj='modules.depth_exceeded';
    SELECT * INTO r FROM public.kn_modul_yenile(child_id,1,other_id,'Child',NULL,NULL,1,true,true);
    ASSERT r.status_kodu='UGURLU';
    SELECT * INTO r FROM public.kn_modul_yenile(other_id,2,NULL,'Other',NULL,NULL,1,true,true);
    ASSERT r.status_kodu='UGURLU';
    ASSERT (SELECT count(*)=3 FROM public.rn_modullar WHERE sistem_id=2);
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,other_id,'WRONG_SYSTEM','Wrong',NULL,NULL,1);
    ASSERT r.mesaj='modules.invalid_parent';
    SELECT * INTO r FROM public.kn_modul_yenile(root_id,1,NULL,' ',NULL,NULL,1,true,true);
    ASSERT r.mesaj='modules.invalid_name';
    UPDATE public.rn_modullar SET route='/existing' WHERE id=root_id;
    SELECT * INTO r FROM public.kn_modul_qrupu_yarat_4(1,root_id,'ROUTED','Routed',NULL,NULL,1);
    ASSERT r.mesaj='modules.route_parent';
END;
$$;
