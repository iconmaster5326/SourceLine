package com.iconmaster.srcline;

import com.iconmaster.sbcore.execute.VirtualMachine;
import com.iconmaster.source.Source;
import com.iconmaster.source.SourceOptions;
import com.iconmaster.source.SourceOutput;
import com.iconmaster.source.compile.CompileData;
import com.iconmaster.source.compile.Expression;
import com.iconmaster.source.compile.SourceCompiler;
import com.iconmaster.source.element.Element;
import com.iconmaster.source.element.Rule;
import com.iconmaster.source.exception.SourceException;
import com.iconmaster.source.link.Linker;
import com.iconmaster.source.parse.Parser;
import com.iconmaster.source.prototype.Field;
import com.iconmaster.source.prototype.Function;
import com.iconmaster.source.prototype.SourcePackage;
import com.iconmaster.source.tokenize.TokenRule;
import com.iconmaster.source.tokenize.Tokenizer;
import com.iconmaster.source.validate.Validator;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author iconmaster
 */
public class SourceLine {
	public static void init() {
		Linker.registerPlatform(new PlatformSourceLine());
	}
	
	public static SourceOutput compile(String in) {
		SourceOptions so = new SourceOptions(in, "SourceLine", true);
		so.noRunAssemble = true;
		SourceOutput sout = Source.execute(so);
		
		return sout;
	}

	public static void mainFunc(String[] args) {
		init();
		SourceOutput sout = compile("");
		SourceLine sl = new SourceLine(sout.pkg, new SLOptions(System.out, System.in, System.err));
		sl.beginSession();
	}
	
	public SourcePackage pkg;
	public SLOptions opts;
	
	public PrintWriter out;
	public PrintWriter err;
	public Scanner in;
	
	public VirtualMachine vm;
	
	public SourceLine(SourcePackage pkg, SLOptions opts) {
		this.pkg = pkg;
		this.opts = opts;
		
		out = new PrintWriter(opts.out);
		err = new PrintWriter(opts.err);
		in = new Scanner(opts.in);
	}
	
	public void beginSession() {
		println("Welcome to SourceLine!");
		println("For help, type \":h\".");
		while (true) {
			String line = getInput();
			try {
				execute(line);
			} catch (Exception ex) {
				String errName = ex.getClass().getCanonicalName();
				err.print("INTERNAL ERROR: ");
				err.print(errName.substring(errName.lastIndexOf('.')+1));
				err.print(": ");
				err.print(ex.getMessage());
				err.println();
				err.flush();
				
				Logger.getLogger(SourceLine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
	public String getInput() {
		print("> ");
		String line = in.nextLine();
		if (line.endsWith("\\")) {
			line = line.substring(0,line.length()-1);
			line+="\n";
			while (true) {
				print(">>");
				String line2 = in.nextLine();
				
				if (!line2.endsWith("\\")) {
					line+=line2;
					break;
				}
				
				line2 = line2.substring(0,line2.length()-1);
				line+=line2;
				line+="\n";
			}
		}
		return line;
	}
	
	public void print(String s) {
		out.print(s);
		out.flush();
	}
	
	public void println(String s) {
		out.println(s);
		out.flush();
	}
	
	public void execute(String input) {
		if (input.startsWith(":")) {
			String[] args = input.split("%s+");
			char cmd = args[0].charAt(1);
			
			switch (cmd) {
				case 'h':
				case 'H':
					println("SourceLine is a command line interpreter for the Source language.");
					println("Source expressions and statements will be executed.");
					println("Functions, fields, etc. will be added to the execution workspace.");
					println("Put a \\ at the end of a line to continue typing on another line.");
					println("To exit this program, type \":q\".");
					break;
				case 'q':
				case 'Q':
					System.exit(0);
			}
			
			return;
		}
		
		try {
			ArrayList<Element> tokens = Tokenizer.tokenize(input);
			ArrayList<Element> parsed = Parser.parse(tokens);
			
			final int EXEC = 0;
			final int LINE = 1;
			final int DEF = 2;
			final int VAR = 3;
			
			int mode = EXEC;
			if (parsed.isEmpty()) {
				return;
			} else if (parsed.size()==1) {
				Element e = parsed.get(0);
				if (e.type instanceof TokenRule) {
					switch ((TokenRule)e.type) {
						default:
							mode = EXEC;
							break;
					}
				} else {
					switch ((Rule)e.type) {
						case ASSIGN:
						case LOCAL:
						case LOCAL_ASN:
							mode = VAR;
							break;
						case FUNC:
						case FIELD:
						case FIELD_ASN:
						case ITERATOR:
							mode = DEF;
							break;
						case IFBLOCK:
						case WHILE:
						case REPEAT:
						case FOR:
						case ADD_ASN:
						case MUL_ASN:
						case SUB_ASN:
						case DIV_ASN:
							mode = LINE;
							break;
						default:
							mode = EXEC;
							break;
					}
				}
			} else {
				mode = LINE;
			}
			
			Element e = parsed.get(0);
			
			switch (mode) {
				case EXEC:
					ArrayList<SourceException> errs = Validator.validate(parsed, Validator.Scope.RVALUE);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					
					Function fn = new Function("%EXEC", new ArrayList<>(), null);
					
					Element e2 = new Element(null, Rule.FCALL);
					e2.args[0] = "print";
					e2.args[1] = new ArrayList();
					((ArrayList)e2.args[1]).add(e);
					ArrayList<Element> a2 = new ArrayList<>();
					a2.add(e2);
					fn.rawCode = a2;
					
					pkg.addFunction(fn);
					errs = SourceCompiler.compile(pkg);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					
					if (vm==null) {
						vm = new VirtualMachine(pkg);
					}
					vm.loadFunction(fn);
					vm.run();
					break;
				case LINE:
					errs = Validator.validate(parsed, Validator.Scope.CODE);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					
					fn = new Function("%EXEC", new ArrayList<>(), null);

					fn.rawCode = parsed;
					
					pkg.addFunction(fn);
					errs = SourceCompiler.compile(pkg);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					
					if (vm==null) {
						vm = new VirtualMachine(pkg);
					}
					vm.loadFunction(fn);
					vm.run();
					break;
				case DEF:
					errs = Validator.validate(parsed, Validator.Scope.GLOBAL);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					
					pkg.parse(parsed);
					errs = SourceCompiler.compile(pkg);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					break;
				case VAR:
					errs = Validator.validate(parsed, Validator.Scope.CODE);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					
					ArrayList<Element> a = (ArrayList<Element>) e.args[0];
					a2 = (ArrayList<Element>) e.args[1];
					
					int i=0;
					for (Element e3 : a) {
						Element e4 = a2.get(i);
						String rvar = SourceCompiler.resolveLValueRaw(new CompileData(pkg), e3);
						if (rvar!=null) {
							Field f = pkg.getField(rvar);
							if (f==null) {
								f = new Field(rvar, null);
								pkg.addField(f);
							}
							Expression expr2 = SourceCompiler.compileExpr(new CompileData(pkg), rvar, e4);
							f.setType(expr2.type);
							f.setCompiled(expr2);
							
							if (vm==null) {
								vm = new VirtualMachine(pkg);
							}
							Expression expr = f.getValue();
							Function fn2 = new Function("%field", new ArrayList<>(), null);
							pkg.addFunction(fn2);
							fn2.setCompiled(expr);

							vm.loadFunction(fn2);
							vm.run();
						}
						i++;
					}
					
					errs = SourceCompiler.compile(pkg);
					if (!errs.isEmpty()) {
						for (SourceException ex : errs) {
							err.print("ERROR: ");
							err.print(ex.getMessage());
							err.println();
						}
						err.flush();
						return;
					}
					break;
			}
		} catch (SourceException ex) {
			err.print("ERROR: ");
			err.print(ex.getMessage());
			err.println();
			err.flush();
		}
	}
}
