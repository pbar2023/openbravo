CREATE OR REPLACE function NOW
RETURN DATE
AS
/*************************************************************************
* The contents of this file are subject to the Openbravo  Public  License
* Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
* Version 1.1  with a permitted attribution clause; you may not  use this
* file except in compliance with the License. You  may  obtain  a copy of
* the License at http://www.openbravo.com/legal/license.html
* Software distributed under the License  is  distributed  on  an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific  language  governing  rights  and  limitations
* under the License.
* The Original Code is Openbravo ERP.
* The Initial Developer of the Original Code is Openbravo SLU
* All portions are Copyright (C) 2001-2018 Openbravo SLU
* All Rights Reserved.
* Contributor(s):  ______________________________________.
************************************************************************/
BEGIN
  RETURN SYSDATE;
END NOW;
/-- END NOW

DECLARE
  isPresent number;
BEGIN
  SELECT count(*) INTO isPresent
  FROM USER_PROCEDURES WHERE object_name ='HEX_TO_INT' AND object_type = 'FUNCTION';
  IF isPresent != 0 THEN
    EXECUTE IMMEDIATE 'drop function hex_to_int';
  end if;
END;
/-- END

CREATE OR REPLACE FUNCTION ad_script_execute (param_Message VARCHAR2)
  RETURN VARCHAR2
AS
/*************************************************************************
* The contents of this file are subject to the Openbravo  Public  License
* Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
* Version 1.1  with a permitted attribution clause; you may not  use this
* file except in compliance with the License. You  may  obtain  a copy of
* the License at http://www.openbravo.com/legal/license.html
* Software distributed under the License  is  distributed  on  an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific  language  governing  rights  and  limitations
* under the License.
* The Original Code is Openbravo ERP.
* The Initial Developer of the Original Code is Openbravo SLU
* All portions are Copyright (C) 2001-2008 Openbravo SLU
* All Rights Reserved.
* Contributor(s):  ______________________________________.
************************************************************************/
 v_Message       VARCHAR2(4000) := '';
 v_ResultStr     VARCHAR2(4000) := '';
 TYPE RECORD IS REF CURSOR;
 Cur_Script RECORD;
BEGIN
    v_Message := param_Message;
    FOR Cur_Script IN 
      (SELECT STRSQL, SEQNO FROM AD_SCRIPT_SQL ORDER BY SEQNO)
    LOOP 
    BEGIN 
      EXECUTE IMMEDIATE(Cur_Script.STRSQL) ; 
    EXCEPTION 
    WHEN OTHERS THEN 
      
      if (coalesce(length(v_Message),0)!=0) then
        v_Message:=substr(v_Message||'<br><br>',1,2000);
      end if;
      v_Message := substr(v_Message||'@SQLScriptError@ '||Cur_Script.SeqNo||'. @Executing@'||Cur_Script.strSQL||'<br>'||SQLERRM,1,2000);
    END;
  END LOOP;

 IF( LENGTH(v_Message) > 0 ) THEN
    DBMS_OUTPUT.PUT_LINE('Script errors: ' || v_Message);
 END IF;
 return substr(coalesce(v_ResultStr,'') || coalesce(v_Message,''), 1, 2000);
END ad_script_execute;
/-- END

CREATE OR REPLACE FUNCTION ad_script_drop_recreate_index (p_seqNoStart NUMBER)
  RETURN NUMBER
AS
/*************************************************************************
* The contents of this file are subject to the Openbravo  Public  License
* Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
* Version 1.1  with a permitted attribution clause; you may not  use this
* file except in compliance with the License. You  may  obtain  a copy of
* the License at http://www.openbravo.com/legal/license.html
* Software distributed under the License  is  distributed  on  an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific  language  governing  rights  and  limitations
* under the License.
* The Original Code is Openbravo ERP.
* The Initial Developer of the Original Code is Openbravo SLU
* All portions are Copyright (C) 2001-2008 Openbravo SLU
* All Rights Reserved.
* Contributor(s):  ______________________________________.
************************************************************************/
 v_seqNo         NUMBER; 
 v_strSql        VARCHAR2(4000) := '';
 v_strTemp       VARCHAR2(4000) := '';
 v_Message       VARCHAR2(4000) := '';
 v_ResultStr     VARCHAR2(4000) := '';
 TYPE RECORD IS REF CURSOR;
 Cur_UniqueIndex  RECORD;
 Cur_IndexColumns RECORD;
BEGIN
    v_seqNo := p_seqNoStart;
    FOR Cur_UniqueIndex IN (SELECT i.INDEX_NAME, i.TABLE_NAME, i.TABLESPACE_NAME, CONSTRAINT_TYPE
                 FROM USER_INDEXES I left join USER_CONSTRAINTS C1 on c1.INDEX_NAME=I.INDEX_NAME
                 WHERE UNIQUENESS='UNIQUE' AND INDEX_TYPE='NORMAL' AND TABLE_TYPE='TABLE'
               --AND CONSTRAINT_TYPE != 'U'
               ORDER BY INDEX_NAME)

    LOOP
      v_seqNo:=v_seqNo + 1;
      INSERT
      INTO AD_SCRIPT_SQL VALUES (v_seqNo, 'DROP INDEX '||Cur_UniqueIndex.INDEX_NAME) ;

   IF Cur_UniqueIndex.CONSTRAINT_TYPE != 'P' THEN
    v_strSql:='CREATE INDEX '||Cur_UniqueIndex.INDEX_NAME||' ON '||Cur_UniqueIndex.TABLE_NAME||'(';
       v_strTemp:='';
       FOR Cur_IndexColumns IN
         (SELECT COLUMN_NAME
       FROM USER_IND_COLUMNS
       WHERE INDEX_NAME=Cur_UniqueIndex.INDEX_NAME
       ORDER BY COLUMN_POSITION)
       LOOP
         v_strTemp:=v_strTemp ||','|| Cur_IndexColumns.COLUMN_NAME;
       END LOOP;
       v_strSql:=v_strSql || SUBSTR(v_strTemp, 2, 2000) || ') TABLESPACE '||Cur_UniqueIndex.TABLESPACE_NAME;
       INSERT INTO AD_SCRIPT_SQL VALUES(v_seqNo+100000, v_strSql) ;
   END IF;
 END LOOP;
 return v_seqNo;
END ad_script_drop_recreate_index;
/-- END

CREATE OR REPLACE FUNCTION AD_ORG_CHK_DOCUMENTS(p_header_table IN VARCHAR2, p_lines_table IN VARCHAR2, p_document_id IN VARCHAR2, p_header_column_id IN VARCHAR2, p_lines_column_id IN VARCHAR2) 
 RETURN NUMBER
 AS
/*************************************************************************
* The contents of this file are subject to the Openbravo  Public  License
* Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
* Version 1.1  with a permitted attribution clause; you may not  use this
* file except in compliance with the License. You  may  obtain  a copy of
* the License at http://www.openbravo.com/legal/license.html
* Software distributed under the License  is  distributed  on  an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific  language  governing  rights  and  limitations
* under the License.
* The Original Code is Openbravo ERP.
* The Initial Developer of the Original Code is Openbravo SLU
* All portions are Copyright (C) 2008-2009 Openbravo SLU
* All Rights Reserved.
* Contributor(s):  ______________________________________.
************************************************************************/
   v_org_header_id ad_org.ad_org_id%TYPE;
   v_isbusinessunit ad_orgtype.isbusinessunit%TYPE;
   v_islegalentity ad_orgtype.islegalentity%TYPE;
   v_is_included NUMBER:=0;
  
   TYPE RECORD IS REF CURSOR;
   cur_doc_lines RECORD;
 
   v_line_org VARCHAR2(32);
   v_org_line_id VARCHAR2(32);
 BEGIN  
	 
   -- Gets the Business Unit or Legal Entity of the document
   SELECT AD_GET_DOC_LE_BU(p_header_table, p_document_id, p_header_column_id, NULL)
   INTO v_org_header_id
   FROM DUAL;
 
   -- Check the lines belong to the same BU or LE as the header
   OPEN cur_doc_lines FOR
   'SELECT DISTINCT('||p_lines_table||'.ad_org_id) AS v_line_org
    FROM '||p_header_table||', '||p_lines_table||'
    WHERE '||p_header_table||'.'||p_header_column_id||' = '||p_lines_table||'.'||p_lines_column_id||'
    AND '||p_lines_table||'.ad_org_id<>'||''''||v_org_header_id||'''
    AND '||p_lines_table||'.'||p_lines_column_id||'='''||p_document_id||'''';    
    LOOP
      FETCH cur_doc_lines INTO v_line_org;
      EXIT WHEN cur_doc_lines%NOTFOUND;
 
      SELECT ad_orgtype.isbusinessunit, ad_orgtype.islegalentity
      INTO v_isbusinessunit, v_islegalentity
      FROM AD_Org, AD_OrgType
      WHERE AD_Org.AD_OrgType_ID=AD_OrgType.AD_OrgType_ID
      AND AD_Org.AD_Org_ID=v_line_org;
      
      v_org_line_id:=v_line_org;
      -- Gets recursively the organization parent until finding a Business Unit or a Legal Entity
      WHILE (v_isbusinessunit='N' AND v_islegalentity='N') LOOP
        SELECT hh.parent_id, ad_orgtype.isbusinessunit, ad_orgtype.islegalentity
        INTO v_org_line_id, v_isbusinessunit, v_islegalentity
        FROM ad_org, ad_orgtype, ad_treenode pp, ad_treenode hh
        WHERE pp.node_id = hh.parent_id
        AND hh.ad_tree_id = pp.ad_tree_id
        AND pp.node_id=ad_org.ad_org_id
        AND hh.node_id=v_org_line_id
        AND ad_org.ad_orgtype_id=ad_orgtype.ad_orgtype_id
        AND ad_org.isready='Y'
        AND  EXISTS (SELECT 1 FROM ad_tree WHERE ad_tree.treetype='OO' AND hh.ad_tree_id=ad_tree.ad_tree_id AND hh.ad_client_id=ad_tree.ad_client_id);     
      END LOOP;
      
     IF (v_org_line_id<>v_org_header_id) THEN
       v_is_included:=-1;
     END IF;
     EXIT WHEN v_is_included=-1;
 
    END LOOP; 
   CLOSE cur_doc_lines;
 
  RETURN v_is_included;
 
END AD_ORG_CHK_DOCUMENTS;
/-- END

CREATE OR REPLACE FUNCTION AD_ORG_CHK_DOC_PAYMENTS(p_header_table IN VARCHAR2, p_lines_table IN VARCHAR2, p_document_id IN VARCHAR2, p_header_column_id IN VARCHAR2, p_lines_column_id IN VARCHAR2, p_lines_column_payment_id IN VARCHAR2) 
 RETURN NUMBER
 AS
/*************************************************************************
* The contents of this file are subject to the Openbravo  Public  License
* Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
* Version 1.1  with a permitted attribution clause; you may not  use this
* file except in compliance with the License. You  may  obtain  a copy of
* the License at http://www.openbravo.com/legal/license.html
* Software distributed under the License  is  distributed  on  an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific  language  governing  rights  and  limitations
* under the License.
* The Original Code is Openbravo ERP.
* The Initial Developer of the Original Code is Openbravo SLU
* All portions are Copyright (C) 2008-2009 Openbravo SLU
* All Rights Reserved.
* Contributor(s):  ______________________________________.
************************************************************************/
   v_org_header_id ad_org.ad_org_id%TYPE;
   v_isbusinessunit ad_orgtype.isbusinessunit%TYPE;
   v_islegalentity ad_orgtype.islegalentity%TYPE;
   v_is_included NUMBER:=0;
 
 
   TYPE RECORD IS REF CURSOR;
   cur_doc_lines_payment RECORD;
 
   v_line_org_payment VARCHAR2(32);
   v_org_payment_line_id VARCHAR2(32);
 BEGIN
 
   -- Gets the Business Unit or Legal Entity of the document
   SELECT AD_GET_DOC_LE_BU(p_header_table, p_document_id, p_header_column_id, NULL)
   INTO v_org_header_id
   FROM DUAL;
 
   -- Check the payments of the lines belong to the same BU or LE as the document header
   OPEN cur_doc_lines_payment FOR
   'SELECT DISTINCT(C_DEBT_PAYMENT.ad_org_id) AS v_line_org_payment
    FROM '||p_header_table||', '||p_lines_table||', C_DEBT_PAYMENT
    WHERE '||p_header_table||'.'||p_header_column_id||' = '||p_lines_table||'.'||p_lines_column_id||'
    AND C_DEBT_PAYMENT.C_DEBT_PAYMENT_ID='||p_lines_table||'.'||p_lines_column_payment_id||'
    AND '||p_lines_table||'.ad_org_id<>'||''''||v_org_header_id||'''
    AND '||p_lines_table||'.'||p_lines_column_id||'='''||p_document_id||'''';
 
 
   LOOP
    FETCH cur_doc_lines_payment INTO v_line_org_payment;
    EXIT WHEN cur_doc_lines_payment%NOTFOUND;
 

    SELECT ad_orgtype.isbusinessunit, ad_orgtype.islegalentity
    INTO v_isbusinessunit, v_islegalentity
    FROM AD_Org, AD_OrgType
    WHERE AD_Org.AD_OrgType_ID=AD_OrgType.AD_OrgType_ID
    AND AD_Org.AD_Org_ID=v_line_org_payment;

    v_org_payment_line_id:=v_line_org_payment;
    -- Gets recursively the organization parent until finding a Business Unit or a Legal Entity
    WHILE (v_isbusinessunit='N' AND v_islegalentity='N') LOOP
      SELECT hh.parent_id, ad_orgtype.isbusinessunit, ad_orgtype.islegalentity
      INTO v_org_payment_line_id, v_isbusinessunit, v_islegalentity
      FROM ad_org, ad_orgtype, ad_treenode pp, ad_treenode hh
      WHERE pp.node_id = hh.parent_id
      AND hh.ad_tree_id = pp.ad_tree_id
      AND pp.node_id=ad_org.ad_org_id
      AND hh.node_id=v_org_payment_line_id
      AND ad_org.ad_orgtype_id=ad_orgtype.ad_orgtype_id
      AND ad_org.isready='Y'
      AND  EXISTS (SELECT 1 FROM ad_tree WHERE ad_tree.treetype='OO' AND hh.ad_tree_id=ad_tree.ad_tree_id AND hh.ad_client_id=ad_tree.ad_client_id);     
    END LOOP;

    IF (v_org_payment_line_id<>v_org_header_id) THEN
      v_is_included:=-1;
    END IF;
    EXIT WHEN v_is_included=-1;

   END LOOP; 
   CLOSE cur_doc_lines_payment;
 
   RETURN v_is_included;
 
END AD_ORG_CHK_DOC_PAYMENTS;
/-- END

CREATE OR REPLACE VIEW AD_INTEGER AS 
SELECT to_number(a.value) AS value
   FROM ( SELECT ( SELECT count(*) AS count
                   FROM ad_element
                  WHERE ad_element.ad_element_id <= e.ad_element_id) AS value
           FROM ad_element e) a
  WHERE a.value <= 1024
/-- END

--update parent reference for old modules
BEGIN
   EXECUTE IMMEDIATE 'alter trigger ad_reference_mod_trg disable';
END;
/-- END

update ad_reference
   set parentreference_id =( CASE VALIDATIONTYPE WHEN 'S' THEN '30' WHEN 'L' THEN '17' WHEN 'T' THEN '18' end)
   where validationtype in ('S','L','T')
   and parentreference_id is null
/-- END

BEGIN
   EXECUTE IMMEDIATE 'alter trigger ad_reference_mod_trg enable';
END;
/-- END
 
create or replace
FUNCTION AD_Execute_Function(p_code IN VARCHAR2) RETURN VARCHAR2
AS
/*************************************************************************
* The contents of this file are subject to the Openbravo  Public  License
* Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
* Version 1.1  with a permitted attribution clause; you may not  use this
* file except in compliance with the License. You  may  obtain  a copy of
* the License at http://www.openbravo.com/legal/license.html
* Software distributed under the License  is  distributed  on  an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific  language  governing  rights  and  limitations
* under the License.
* The Original Code is Openbravo ERP.
* The Initial Developer of the Original Code is Openbravo SLU
* All portions are Copyright (C) 2012 Openbravo SLU
* All Rights Reserved.
* Contributor(s):  ______________________________________.
************************************************************************/

  v_response varchar2(32);
begin
   execute immediate 'begin :1 := '||p_code||'; end;' using out v_response;
   return v_response;
end;
/-- END

