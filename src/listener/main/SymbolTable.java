package listener.main;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable.Type;
import static listener.main.BytecodeGenListenerHelper.*;


public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int id;
		int initVal;
		
		public VarInfo(Type type,  int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type,  int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
	}
	
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	
		
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;
	
	SymbolTable(){
		initFunDecl();
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_lsymtable.clear(); //clear lsymtable
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;		
	}
	
	void putLocalVar(String varname, Type type){
		//<Fill here> �� �ѹ� ���� �غ���
		VarInfo lvar = new VarInfo(type,this._localVarID); //�������������� VarInfo ��ü ����
		this._localVarID++; //�������� �ϳ��� ��������� �ּҸ� �ϳ� ����������
		this._lsymtable.put(varname, lvar); //�ɺ����̺� ����
	}
	
	void putGlobalVar(String varname, Type type){
		//<Fill here> ���� ������ ������� �۷ι����� �������ֱ�.
		VarInfo gvar = new VarInfo(type,this._globalVarID);
		this._globalVarID++;
		this._lsymtable.put(varname, gvar);
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here> initVar�� �־������� �������� ����
		VarInfo lvar = new VarInfo(type,this._localVarID,initVar);
		this._localVarID++;
		this._lsymtable.put(varname, lvar);
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here> initVar�� �־����� �� �������� ����
		VarInfo gvar = new VarInfo(type,this._globalVarID,initVar);
		this._globalVarID++;
		this._lsymtable.put(varname, gvar);
	}
	
	void putParams(MiniCParser.ParamsContext params) {
		for(int i = 0; i < params.param().size(); i++) {
		//<Fill here>
			//�Ű������� ���������� ����. �׷��Ƿ� �Ű������� �������� ���ϳ��� �����ش�.
			String paramType = params.param().get(i).type_spec().getText();
			String paramName = getParamName(params.param().get(i));
			if(paramType.equals("int")) {
				putLocalVar(paramName,Type.INT);	
			}
			//�迭�� ���ٰ� ����!
//			else if (paramType.equals("int[]")) {
//				putLocalVar(paramName,Type.INTARRAY);
//			}
			else {
				putLocalVar(paramName,Type.ERROR);
			}
//			
		}
	}
	
	private void initFunTable() {
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {		
		// <Fill here>
		return this._fsymtable.get(fname).sigStr;
	}

	public String getFunSpecStr(Fun_declContext ctx) {
		// <Fill here>	
		return getFunSpecStr(ctx.IDENT().getText());
	}
	
	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = getFunName(ctx);
		String argtype = "";	
		String rtype = "";
		String res = "";
		
		// <Fill here>	
		String params = getParamTypesText(ctx.params());
		params = params.replaceAll("int", "I");
		params = params.replaceAll("void","V");
		argtype = params;

		if (isVoidF(ctx)) {
			rtype ="V";
		}
		else {
			rtype ="I";
		}
		
		res =  fname + "(" + argtype + ")" + rtype;
		
		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);
		
		return res;
	}
	
	String getVarId(String name){
		// <Fill here>	�ؿ�����Ȱ���
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return Integer.toString(lvar.id);
		}
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return Integer.toString(gvar.id);
		}
		return "Error";
	}
	
	Type getVarType(String name){
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return lvar.type;
		}
		
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return gvar.type;
		}
		
		return Type.ERROR;	
	}
	String newLabel() {
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		// <Fill here>	
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
	
}
