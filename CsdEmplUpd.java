package com.l3;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TBoolean;
import com.temenos.api.TField;
import com.temenos.api.TString;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.tables.csdemployermaster.CsdEmployerMasterRecord;
import com.temenos.t24.api.tables.csdemployermaster.SubNameClass;
import com.temenos.t24.api.tables.csdempmasterdept.CsdEmpMasterDeptRecord;
import com.temenos.t24.api.tables.csdempmasterdept.CsdEmpMasterDeptTable;

/**
 * TODO: Document me!
 *
 * @author sourabh
 *
 */
public class CsdEmplUpd extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String recordId, TStructure record, TStructure lastLiveRecord) {
        // TODO Auto-generated method stub
    }

    @Override
    public TValidationResponse validateRecord(String application, String recordId, TStructure record,
            TStructure lastLiveRecord) {
        // TODO Auto-generated method stub

        RecordLifecycle Curr_Common = new RecordLifecycle(this);
        String id_old = Curr_Common.loadCommonVariables().getRecIdOld();
        String Curr_Function = Curr_Common.loadCommonVariables().getFunction();
        String Allowed_Function = "IAELSC";
        if (Allowed_Function.contains(Curr_Function)) {
        } else {
            throw new T24CoreException(null, "EB-NO.FUNCTION");
        }

        // get the Employer tables current record values (R.NEW)
        CsdEmployerMasterRecord empmst = new CsdEmployerMasterRecord(record);
        TField empsts = empmst.getEmpStatus();

        // Get the Employer tables current records, U-auth values (R.NEW.LAST)
        CsdEmployerMasterRecord empmsthld = new CsdEmployerMasterRecord(lastLiveRecord);

        // if and only if the lasts status and current status are different,
        // date to be defaulted.

        // Perform OPF
        DataAccess DAFile = new DataAccess(this);
        TField OldEmpStatus = new TField();
        CsdEmployerMasterRecord HistRec = new CsdEmployerMasterRecord(this);

        if (!(id_old.isEmpty())) {
            // Only if history exist, otherwise read will result in error.
            try {
                HistRec = new CsdEmployerMasterRecord(DAFile.getRecord("EB.CSD.EMPLOYER.MASTER", recordId));
                OldEmpStatus = HistRec.getEmpStatus();
            } catch (T24CoreException e) {
                // OldEmpStatus is already set to null. as sUch no reset.
            }
        }

        // Parse the Dept Code of historical record and park in an array for
        // reference building/updating the concat table.
        List<SubNameClass> Sub_Code_Old = HistRec.getSubName();
        int Sub_Code_Old_C = Sub_Code_Old.size();
        int Sub_Code_Old_i = 0;
        List<String> Old_ids = new ArrayList<String>();
        for (Sub_Code_Old_i = 0; (Sub_Code_Old_i <= Sub_Code_Old_C - 1); Sub_Code_Old_i++) {
            Old_ids.add(Sub_Code_Old.get(Sub_Code_Old_i).getSubCode().toString());
        }

        // Parse the Dept Code of ihld/inau record and park in an array for
        // reference building/updating the concat table.
        List<SubNameClass> Sub_Code_Hld = empmsthld.getSubName();
        Sub_Code_Old_C = Sub_Code_Hld.size();
        Sub_Code_Old_i = 0;
        List<String> Hld_ids = new ArrayList<String>();
        for (Sub_Code_Old_i = 0; (Sub_Code_Old_i <= Sub_Code_Old_C - 1); Sub_Code_Old_i++) {
            Hld_ids.add(Sub_Code_Hld.get(Sub_Code_Old_i).getSubCode().toString());
        }

        // Parse the Dept Code of current record and park in an array for
        // reference.
        List<SubNameClass> Sub_Code = empmst.getSubName();
        int Sub_Code_Count = Sub_Code.size();
        int ii = 0;
        String SubCde = new String();
        CsdEmpMasterDeptRecord LookupRec = new CsdEmpMasterDeptRecord(this);
        TField LookupRec_EmpId = new TField();
        DataAccess DAFileL = new DataAccess(this);
        CsdEmpMasterDeptTable Tab_empmst = new CsdEmpMasterDeptTable(this);
        List<String> New_ids = new ArrayList<String>();
        List<String> Auth_ids = new ArrayList<String>();
        List<String> Dup_ids = new ArrayList<String>();
        boolean bl_id;

        // parse the dept codes for entries and update
        for (ii = 0; ii <= (Sub_Code_Count - 1); ii++) {
            SubCde = Sub_Code.get(ii).getSubCode().toString();
            bl_id = Dup_ids.contains(SubCde); // Check if exists ?
            if (bl_id) {
                // already exists. thats duplicate.
                List<String> Err_id = new ArrayList<String>();
                Err_id = new ArrayList<String>();
                Err_id.add("AA-DUPLICATE.VALUES.NOT.ALLOWED");
                Err_id.add(SubCde);
                Sub_Code.get(ii).getSubCode().setError(Err_id.toString());
            } else {
                // new entry? keep it. no problem
                Dup_ids.add(SubCde);
            }

            try {
                // Try to Read-in the Sub Code data and check whether its equal
                // to current record id
                LookupRec = new CsdEmpMasterDeptRecord(DAFileL.getRecord("EB.CSD.EMP.MASTER.DEPT", SubCde));
                LookupRec_EmpId = LookupRec.getEmpId();
                if ((LookupRec_EmpId.toString()).equals(recordId)) {
                    // Record exists and are tagged with current id ? Fine then,
                    // No problem.
                    Auth_ids.add(SubCde);
                } else {
                    // Record exists and are NOT tagged with current id ?
                    List<String> Err_id = new ArrayList<String>();
                    Err_id = new ArrayList<String>();
                    Err_id.add("EB-ALREADY.EXISTS.IN");
                    Err_id.add(LookupRec_EmpId.toString());
                    Sub_Code.get(ii).getSubCode().setError(Err_id.toString());
                }
            } catch (T24CoreException e) {
                New_ids.add(SubCde);
            }
        }

        int New_ids_C = New_ids.size();
        for (ii = 0; ii <= New_ids_C - 1; ii++) {
            // Reuse SubCde var.
            SubCde = New_ids.get(ii);
            LookupRec.setEmpId(recordId);
            try {
                Tab_empmst.write(SubCde, LookupRec);
            } catch (T24IOException ee) {
            }
        }

        // Check for removal of Dept Code ids from old vs new. if yes, delete
        // the old ones from concat files.
        List<String> Del_list = new ArrayList<String>();
        if (Hld_ids.isEmpty()) { // Is the record amended from Unauth stage ?
            Old_ids.addAll(New_ids);
            Del_list = new ArrayList<String>(Old_ids);
            Del_list.removeAll(New_ids);
            Del_list.removeAll(Auth_ids);
        } else {
            Hld_ids.addAll(New_ids);
            Del_list = new ArrayList<String>(Hld_ids);
            Del_list.removeAll(Dup_ids);
        }

        int Del_list_C = Del_list.size();
        for (ii = 0; ii <= Del_list_C - 1; ii++) {
            // reuse SubCde
            SubCde = Del_list.get(ii);
            try {
                Tab_empmst.delete(SubCde);
            } catch (T24IOException ee) {
            }
        }

        // Verify for status change and reset the Emp Date values.
        String Str_empsts = empsts.toString();
        String Str_empsts_old = OldEmpStatus.toString();
        if (!(Str_empsts.equals(Str_empsts_old))) {
            // if status are not equal reset the dates.
            Date date_rec = new Date(this);
            DatesRecord date_rec_all = date_rec.getDates();
            empmst.setEmpDate(date_rec_all.getToday());
        } else {
            // Does the User changed the status and reverted to old status ?
            if (!(empmst.getEmpDate().toString().equals(HistRec.getEmpDate().toString()))) {
                // If Yes, reset to the Old Status date, provided history
                // exists.
                if (!(HistRec.getEmpDate().getValue().isEmpty())) {
                    empmst.setEmpDate(HistRec.getEmpDate());
                }
            }
        }

        // reload the changes from (local R.NEW) empmst to standard variable
        // "record" (actual R.New).
        record.set(empmst.toStructure());
        return empmst.getValidationResponse();
    }

    @Override
    public TBoolean updateLookupTable(String application, String recordId, TStructure record, TStructure lastLiveRecord,
            TString lookupTableName, TString key, TString entryToDelete, TString entryToAdd, TBoolean sortAsNumber) {
        // TODO Auto-generated method stub

        return null;
    }

    @Override
    public void updateCoreRecord(String application, String recordId, TStructure record, TStructure lastLiveRecord,
            List<String> versionNames, TBoolean isZeroAuth, List<String> recordIds, List<TStructure> records) {
        // TODO Auto-generated method stub

    }

    @Override
    public String checkId(String idNew) {
        // TODO Auto-generated method stub

        RecordLifecycle Curr_Common = new RecordLifecycle(this);
        String Curr_Function = Curr_Common.loadCommonVariables().getFunction().toString();
        String Allowed_Function = "IAELSC";
        if (Allowed_Function.contains(Curr_Function)) {
        } else {
            throw new T24CoreException(null, "EB-NO.FUNCTION");
        }

        // length less than 12 characters OR id not begin with EM, then post the
        // invalid id error.
        int idloc_len = idNew.length();
        if ((idloc_len < 12) || !(idNew.startsWith("EM"))) {
            throw new T24CoreException(null, "EB-INVALID.ID");
        }
        return idNew;
    }

    @Override
    public String formatDealSlip(String data, TStructure record) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure record) {
        // TODO Auto-generated method stub
        return null;
    }

}
