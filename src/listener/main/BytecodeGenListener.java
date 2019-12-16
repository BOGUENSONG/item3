package listener.main;


import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;


import static listener.main.BytecodeGenListenerHelper.*;
import static listener.main.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();
	
	int tab = 0;
	int label = 0;
	
	// program	: decl+
	
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		symbolTable.initFunDecl();
		
		String fname = getFunName(ctx);
		ParamsContext params;
		
		if (fname.equals("main")) {
			symbolTable.putLocalVar("args", Type.INTARRAY);
		} else {
			symbolTable.putFunSpecStr(ctx);
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			symbolTable.putParams(params);
		}		
	}

	
	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		
		if (isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
			symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putGlobalVar(varName, Type.INT);
		}
	}

	
	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {			
		if (isArrayDecl(ctx)) {
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
			symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));	
		}
		else  { // simple decl
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
		}	
	}

	
	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		String fun_decl = "", var_decl = "";
		
		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				fun_decl += newTexts.get(ctx.decl(i));
			else
				var_decl += newTexts.get(ctx.decl(i));
		}
		
		newTexts.put(ctx, var_decl + fun_decl);
		
		System.out.println(newTexts.get(ctx));
	}	
	
	
	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)				//var_decl
				decl += newTexts.get(ctx.var_decl());
			else							//fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}
	
	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			// <(0) Fill here>		
			else if(ctx.if_stmt() != null) {
				stmt += newTexts.get(ctx.if_stmt()); //if_stmt
			}
			else if(ctx.while_stmt() != null) {
				stmt += newTexts.get(ctx.while_stmt()); //while_stmt
			}
			else {
				stmt += newTexts.get(ctx.return_stmt()); //return_stmt
			}
	}
		newTexts.put(ctx, stmt);
	}
	
	// expr_stmt	: expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}
	
	
	// while_stmt	: WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
			// <(1) Fill here!>
		String loop = this.symbolTable.newLabel();
		String escape = this.symbolTable.newLabel();
		String expr = newTexts.get(ctx.expr());
		String stmt = newTexts.get(ctx.stmt());
		
		String temp ="";
//		temp = loop + ":\n" +expr +  "ifeq " + escape + "\n" + stmt + "goto " + loop + "\n" + escape + ":\n";
		temp = loop + ":\n" + expr + "jg " + escape + "\n"+ stmt + "jmp " + loop + "\n" + escape + ":\n";
		newTexts.put(ctx, temp);

	}
	
	
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
			// <(2) Fill here!>
		String ident = ctx.getChild(1).getText();
		String compound_stmt = newTexts.get(ctx.compound_stmt());
		String type_spec = ctx.type_spec().getText();
		String stmt;
		
		//void타입의 경우 마지막에 return이 붙기 때문에 여기서 처리해준다.
		if (type_spec.equals("void")) {
			stmt = funcHeader(ctx,ident) + compound_stmt + "nop\n" + " pop rbp\n"+" ret\n";
		}
		
		else {
			stmt = funcHeader(ctx,ident) + compound_stmt + " pop rbp\n" + " ret" + "\n" ;
		}

		
		newTexts.put(ctx, stmt);
			// type_spec IDENT '(' params ')' compound_stmt 의 구조
	}
	

	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		
		String variables = "";
		MiniCParser.ParamsContext params = ctx.params();
		for (int i = 0 ; i < params.param().size();i++) {
			symbolTable.push_argStack(); //Stack에 parameter 개수만큼 push한다.
		}
		for (int i = 0,j = -4 ; i < params.param().size();i++) {
			
			variables += String.format(" mov DWORD PTR [rbp%d], %s\n",j,symbolTable.get_argRegister());
			j = j-4;
			
		}
		return symbolTable.getFunSpecStr(fname) + "\n"	
//				+ "\t" + ".limit stack " 	+ getStackSize(ctx) + "\n"
//				+ "\t" + ".limit locals " 	+ getLocalVarSize(ctx) + "\n";
				+ " push rbp\n"
				+ " mov rbp, rsp" + "\n" + variables;
				 	
	}
	
	
	
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}
	
	
	//수정 11-26
	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			String vId = symbolTable.getVarId(ctx);
			varDecl += " mov DWORD PTR [rbp" + vId + "]"+ ", " + ctx.LITERAL().getText() + "\n"; 
//			varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
//					+ "istore_" + vId + "\n";
		}
		
		newTexts.put(ctx, varDecl);
	}

	
	// compound_stmt	: '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		// <(3) Fill here>
		String localDeclAndStmt = "";
		
		for(MiniCParser.Local_declContext l : ctx.local_decl())
			localDeclAndStmt += newTexts.get(l);
		
		
		for(MiniCParser.StmtContext s : ctx.stmt())
			localDeclAndStmt += newTexts.get(s);
		
		newTexts.put(ctx, localDeclAndStmt);
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		String condExpr= newTexts.get(ctx.expr());
		String thenStmt = newTexts.get(ctx.stmt(0));
		
		String lend = symbolTable.newLabel();
		String lelse = symbolTable.newLabel();
		
		
		if(noElse(ctx)) {		
			stmt += condExpr + "\n"
				+ "je " + lend + "\n"
				+ thenStmt + "\n"
				+ lend + ":"  + "\n";	
		}
		else {
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "je " + lelse + "\n"
					+ thenStmt + "\n"
					+ "jmp " + lend + "\n"
					+ lelse + ": " + elseStmt + "\n"
					+ lend + ":"  + "\n";	
		}
		
		newTexts.put(ctx, stmt);
	}
	
	
	// return_stmt	: RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
			// <(4) Fill here>
		String returnStmt;
		
		if(ctx.expr() != null) {	// RETURN expr ';'
			String expr = newTexts.get(ctx.expr());
			returnStmt = expr;
		}
		else{
			// RETURN ';' 이 경우는 반환값이 VOID설정일 때인데, exitFunc_decl에서 관리해주도록 한다.
			returnStmt = "";
		}
			
		
		newTexts.put(ctx, returnStmt);
	}

	
	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";

		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}		
		
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			if(ctx.IDENT() != null) {
				String idName = ctx.IDENT().getText();
				if(symbolTable.getVarType(idName) == Type.INT) {
					expr += " mov " + symbolTable.push_Register() + ", DWORD PTR [rbp" + symbolTable.getVarId(idName) + "] \n";
				}
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
				} else if (ctx.LITERAL() != null) {
					String literalStr = ctx.LITERAL().getText();
					expr += " mov " + symbolTable.push_argStack() + ", " +literalStr + " \n";
				}
			} 
		else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx,expr);			
		}
		else if(ctx.getChildCount() == 3) {	 
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));
				
			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
				
			} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
				
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
		}
		newTexts.put(ctx, expr);
	}


	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		
		expr += newTexts.get(ctx.expr(0)) ;
		switch(ctx.getChild(0).getText()) {
		case "-":
			expr += " neg " + symbolTable.getRegister() + "\n"; break;
		case "--":
			expr += " sub " + symbolTable.getRegister() + ", 1\n";
			break;
		case "++":
			expr += " add " + symbolTable.getRegister() + ", 1\n";
			break;
		case "!":
			expr += " cmp " + symbolTable.getRegister() + ", 0\n"
			+ " sete al\n" + " movzx " + symbolTable.push_Register() +", al\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));
		
		String reg1 = symbolTable.getRegister();
		String reg2 = symbolTable.getRegister();
		switch (ctx.getChild(1).getText()) {

			case "*":
				expr += " imul " + reg1 + ", " + reg2 + "\n";
				symbolTable.push_hasRegName(reg1);
				break;
			case "/":
				expr += " idiv " + reg1 + ", " + reg2 + "\n";
				symbolTable.push_hasRegName(reg1);
				break;
			case "%":
				expr += "Error 어셈블리는 나머지연산없음"; break; //어셈블리는 나머지연산이없다
			case "+":		// expr(0) expr(1) iadd
				expr += " add " + reg1 + ", " +  reg2 + "\n"; 
				symbolTable.push_hasRegName(reg1);
				break;
			case "-":
				expr += " sub " + reg1 + ", " +  reg2 + "\n"; 
				symbolTable.push_hasRegName(reg1);
				break;
			case "==":
				expr += " cmp "+ reg1 + ", " + reg2 + "\n"
						+ " je " + l2 + "\n"
						+ " mov eax 0" + "\n"
						+ " jmp " + lend + "\n"
						+ l2 + ": \n" + " mov eax 1" + "\n"
						+ lend + ": " + "\n";
				symbolTable.push_hasRegName(reg1);
				break;
			case "!=":
				expr += " cmp "+ reg1 + ", " + reg2 + "\n"
						+ " jne " + l2 + "\n"
						+ " mov eax 0" + "\n"
						+ " jmp " + lend + "\n"
						+ l2 + ": \n" + " mov eax 1" + "\n"
						+ lend + ": " + "\n";
				symbolTable.push_hasRegName(reg1);
				break;
			case "<=":
				// <(5) Fill here>
				expr += " cmp "+ reg1 + ", " + reg2 + "\n"
						+ " jle " + l2 + "\n"
						+ " mov eax 0" + "\n"
						+ " jmp " + lend + "\n"
						+ l2 + ": \n" + " mov eax 1" + "\n"
						+ lend + ": " + "\n";
				symbolTable.push_hasRegName(reg1);
				break;
			case "<":
				// <(6) Fill here>
				expr += " cmp "+ reg1 + ", " + reg2 + "\n"
						+ " jl " + l2 + "\n"
						+ " mov eax 0" + "\n"
						+ " jmp " + lend + "\n"
						+ l2 + ": \n" + " mov eax 1" + "\n"
						+ lend + ": " + "\n";
				symbolTable.push_hasRegName(reg1);
				break;

			case ">=":
				// <(7) Fill here>
				expr += " cmp "+ reg1 + ", " + reg2 + "\n"
						+ " jge " + l2 + "\n"
						+ " mov eax 0" + "\n"
						+ " jmp " + lend + "\n"
						+ l2 + ": \n" + " mov eax 1" + "\n"
						+ lend + ": " + "\n";
				symbolTable.push_hasRegName(reg1);

				break;

			case ">":
				// <(8) Fill here>
				expr += " cmp "+ reg1 + ", " + reg2 + "\n"
						+ " jg " + l2 + "\n"
						+ " mov eax 0" + "\n"
						+ " jmp " + lend + "\n"
						+ l2 + ": \n" + " mov eax 1" + "\n"
						+ lend + ": " + "\n";
				symbolTable.push_hasRegName(reg1);
				break;

//			case "and":
//				expr +=  "je "+ lend + "\n"
//						+ "pop" + "\n" + "ldc 0" + "\n"
//						+ lend + ": " + "\n"; break;
//			case "or":
//				// <(9) Fill here>
//				expr += "ifeq" + lend + "\n"
//						+ "pop" + "\n" + "ldc 1" + "\n"
//						+ lend + ": " + "\n"; break;

		}
		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		String fname = getFunName(ctx);		
		expr = newTexts.get(ctx.args()) 
				+ " call " + symbolTable.getFunSpecStr(fname) + "\n";
		
		return expr;
			
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {

		String argsStr = "";
		
		for (int i=0; i < ctx.expr().size() ; i++) {
			argsStr += newTexts.get(ctx.expr(i)) ; 
		}		
		newTexts.put(ctx, argsStr);
	}


}
