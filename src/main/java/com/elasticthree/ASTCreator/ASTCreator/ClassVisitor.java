package com.elasticthree.ASTCreator.ASTCreator;

import java.util.*;


import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import org.apache.log4j.Logger;
import com.elasticthree.ASTCreator.ASTCreator.Objects.AnnotationNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.ClassHasMethodNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.ClassImplementsNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.ClassNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.CommentsNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.InterfaceHasMethodNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.InterfaceNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.ParameterMethodNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.ThrowMethodNodeAST;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;


import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ClassVisitor extends VoidVisitorAdapter<Object> {

	final static Logger logger = Logger.getLogger(ASTCreator.class);
	final static Logger debugLog = Logger.getLogger("debugLogger");

	private String repoURL;
	private String packageFile;
	private long numberOfClasses;
	private long numberOfInterfaces;
	private List<ClassNodeAST> classes;
	private List<InterfaceNodeAST> interfaces;
	private List<String> methodPartParameters;

	public ClassVisitor(String repoURL, String pacName) {
		setRepoURL(repoURL);
		setPackageFile(pacName);
		setNumberOfClasses(0);
		setNumberOfInterfaces(0);
		setClasses(new ArrayList<ClassNodeAST>());
		setInterfaces(new ArrayList<InterfaceNodeAST>());
		setMethodPartParameters(new ArrayList<String>());
	}

	public void setPackageFile(String packageFile) {
		this.packageFile = packageFile;
	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, Object arg) {
		super.visit(n, arg);
		parsing(n);
	}

	private void parsing(ClassOrInterfaceDeclaration n) {
		// It's a Class
		if (!n.isInterface()) {
			parsingClass(n);
		}
		// It's an Interface
		else {
			parsingInterface(n);
		}
	}


	// 每层递归的入口 具体下一层往Ex Try If哪条路走由这个函数判断
	private List<String> parsingMethodPartParameter(Statement ex){
		List<String> res=new ArrayList<String>();

		if(ex.getClass().getTypeName().endsWith("ExpressionStmt")) {
			res.addAll(parsingMethodPartParameter((ExpressionStmt)ex));
		}else if(ex.getClass().getTypeName().endsWith("TryStmt")){
			res.addAll(parsingMethodPartParameter((TryStmt)ex));
		}else if(ex.getClass().getTypeName().endsWith("IfStmt")){
			res.addAll(parsingMethodPartParameter((IfStmt)ex));
		}else if(ex.getClass().getTypeName().endsWith("ForEachStmt")){
			res.addAll(parsingMethodPartParameter((ForEachStmt)ex));
		}else{
			try {
//				throw new Exception("There's still other type of stmt - "+ex.getClass().getTypeName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return res;
	}

	// ExpressionStmt 入口
	private List<String> parsingMethodPartParameter(ExpressionStmt ex){

		List<String> res=new ArrayList<String>();

		Queue<Node> searchQueue = new LinkedList<Node>();
		searchQueue.addAll(ex.getChildNodes());

		while(!searchQueue.isEmpty()){
			Node node = searchQueue.poll();

			searchQueue.addAll(node.getChildNodes());

			if(node instanceof ClassOrInterfaceType){
				res.add(((ClassOrInterfaceType)node).getName().toString());
			}
		}

		return res;
	}

	// TryStmt 入口
	private List<String> parsingMethodPartParameter(TryStmt trys){

		List<String> res=new ArrayList<String>();
//		List<String> resad=new ArrayList<String>();
		if(trys.getResources().size()!=0)
		{
			for (Expression expression: trys.getResources()){
				if (expression.isVariableDeclarationExpr()) {
					NodeList<VariableDeclarator> nodeList = ((VariableDeclarationExpr)expression).getVariables();
					for (VariableDeclarator variableDeclarator: nodeList) {
						res.add((variableDeclarator.getType().toString()));
					}
				}
			}
		}


//		for(int i=0;i<trys.getTryBlock().getStmts().size();i++)
//		{
//			resad.clear();
//			for (String n:parsingMethodPartParameter(trys.getTryBlock().getStmts().get(i))) {
//				resad.add(n);
//			};
//		}

		return res;
	}

	// IfStmt 入口
	private List<String> parsingMethodPartParameter(IfStmt ifs) {
		List<String> res = new ArrayList<String>();

		if (ifs.getThenStmt() != null) {
//			for (Statement stmt : ((BlockStmt) (ifs.getThenStmt())).getStmts()) {
//				res.addAll(parsingMethodPartParameter(stmt));
//			}

			res.addAll(parsingMethodPartParameter(ifs.getThenStmt()));
		}

		if (ifs.getElseStmt() != null) {
//			for (Statement stmt : ((BlockStmt) (ifs.getElseStmt())).getStmts()) {
//				res.addAll(parsingMethodPartParameter(stmt));
//			}
			res.addAll(parsingMethodPartParameter(ifs.getElseStmt().orElse(null)));
		}

		return res;
	}

	// foreachStmt 入口
	private List<String> parsingMethodPartParameter(ForEachStmt fors){
		List<String> res = new ArrayList<String>();

		//Variable 加入
		if(fors.getVariable()!=null)
		{
			NodeList<VariableDeclarator> nodeList = (fors.getVariable().getVariables());
			for (VariableDeclarator variableDeclarator: nodeList) {
				res.add((variableDeclarator.getType().toString()));
			}
		}


		return res;
	}


	private void parsingClass(ClassOrInterfaceDeclaration n) {
		numberOfClasses++;

		Optional<String> name = n.getFullyQualifiedName();
		ClassNodeAST classNode = new ClassNodeAST(getRepoURL(), name.get(),
				getPackageFile());
		NodeList<Modifier> classModifiers = n.getModifiers();
		for(Modifier modifier: classModifiers) {
			Modifier.Keyword keyword = modifier.getKeyword();
			classNode.setAllModifiers(keyword);
		}

		if (n.getExtendedTypes() != null) {
			NodeList<ClassOrInterfaceType> ext = n.getExtendedTypes();
			if (ext.size() > 0) {
				for (ClassOrInterfaceType type: ext) {
					// simple name, extends list, setExtendsClass change to list?
					classNode.setExtendsClass(type.getName().getIdentifier());
				}

			}
		}

		if (n.getImplementedTypes().size() != 0) {
			List<ClassImplementsNodeAST> classImpl = new ArrayList<ClassImplementsNodeAST>();
			for (ClassOrInterfaceType impl : n.getImplementedTypes()) {
				classImpl.add(new ClassImplementsNodeAST(impl.getName().getIdentifier()));
				// logger.info("Implements interfaces: " + impl.getName());
			}
			classNode.setImpl(classImpl);
		}

		if (n.getAnnotations().size() != 0) {
			List<AnnotationNodeAST> classAnn = new ArrayList<AnnotationNodeAST>();

			for (AnnotationExpr ann : n.getAnnotations()) {
				List<MemberValuePair> parameter = new ArrayList<MemberValuePair>();
				if(ann.getChildNodes().size()>1)
				{
					for(int i=1;i<ann.getChildNodes().size();i++)
					{

						if(ann.getChildNodes().get(i) instanceof MemberValuePair) {
							parameter.add((MemberValuePair) ann.getChildNodes().get(i));
						}
					}

				}
				AnnotationNodeAST annotation=new AnnotationNodeAST(ann.toString(),parameter);
				classAnn.add(annotation);
				// logger.info("Implements interfaces: " + impl.getName());
			}
			classNode.setAnnotations(classAnn);
		}

		if (n.getAllContainedComments().size() != 0) {
			List<CommentsNodeAST> classComment = new ArrayList<CommentsNodeAST>();
			for (Comment comment : n.getAllContainedComments()) {
				classComment.add(new CommentsNodeAST(comment.toString()));
				// logger.info("Implements interfaces: " + impl.getName());
			}
			classNode.setComments(classComment);
		}


		if (n.getMembers().size()!=0){
			List<FieldDeclaration> childClassNodes = new ArrayList<FieldDeclaration>();
			for (BodyDeclaration childNode : n.getMembers()) {
				if(childNode instanceof FieldDeclaration)
				{
					childClassNodes.add((FieldDeclaration)childNode);
					FieldDeclaration c=(FieldDeclaration)childNode;
					NodeList<VariableDeclarator> variableDeclarators = c.getVariables();
					for (VariableDeclarator variableDeclarator: variableDeclarators) {
						// type can be explored
						methodPartParameters.add(variableDeclarator.getType().toString());
					}
					classNode.setMethodPartParameters(methodPartParameters);
				}
//
				// logger.info("Implements interfaces: " + impl.getName());
			}
			classNode.setChildClasses(childClassNodes);


		}



		// Method parser
		if (n.getMembers().size() != 0) {
			long numberOfMethodsPerClass = 0;
			List<ClassHasMethodNodeAST> classMethodNode = new ArrayList<ClassHasMethodNodeAST>();
			for (BodyDeclaration member : n.getMembers()) {
				if (member instanceof MethodDeclaration) {
					numberOfMethodsPerClass++;
					MethodDeclaration method = (MethodDeclaration) member;
					ClassHasMethodNodeAST methodClass = new ClassHasMethodNodeAST(
							method.getNameAsString(), getPackageFile());
					methodClass.setReturningType(method.getType().toString());

					if (method.getAllContainedComments().size() != 0) {
						List<CommentsNodeAST> commentsMethod = new ArrayList<CommentsNodeAST>();
						for (Comment comment : method.getAllContainedComments()) {
							commentsMethod.add(new CommentsNodeAST(comment
									.toString()));
						}
						methodClass.setComments(commentsMethod);
					}

					if (method.getAnnotations().size() != 0) {
						List<AnnotationNodeAST> annotatiosMethod = new ArrayList<AnnotationNodeAST>();


						for (AnnotationExpr ann : method.getAnnotations()) {
							List<MemberValuePair> parameter = new ArrayList<MemberValuePair>();
							if(ann.getChildNodes().size()>1)
							{
								for(int i=1;i<ann.getChildNodes().size();i++)
								{

									if(ann.getChildNodes().get(i) instanceof MemberValuePair) {
										parameter.add((MemberValuePair) ann.getChildNodes().get(i));
									}
								}
							}
							AnnotationNodeAST annotation=new AnnotationNodeAST(ann
									.toString(),parameter);
							annotatiosMethod.add(annotation);
						}
						methodClass.setAnnotatios(annotatiosMethod);
					}

					if (method.getParameters().size() != 0) {
						List<ParameterMethodNodeAST> parametersMethod = new ArrayList<ParameterMethodNodeAST>();
						for (Parameter param : method.getParameters()) {
							parametersMethod.add(new ParameterMethodNodeAST(
									param.getType().toString(), param.getName()
											.toString()));
						}
						methodClass.setParameters(parametersMethod);
						//将类内函数的参数传入classNode
						classNode.setMethodParameters(parametersMethod);

					}

					if (method.getThrownExceptions().size() != 0) {
						List<ThrowMethodNodeAST> throwsMethod = new ArrayList<ThrowMethodNodeAST>();
						for (ReferenceType reftype : method.getThrownExceptions()) {
							throwsMethod.add(new ThrowMethodNodeAST(reftype
									.toString()));
						}
						methodClass.setThrowsMethod(throwsMethod);
					}
					Optional<BlockStmt> methodStmt = method.getBody();
					if (methodStmt.isPresent() && methodStmt.get().getStatements().size()!=0)
					{
						List<String> methodNodes = new ArrayList<String>();
						for(int i=0;i<methodStmt.get().getStatements().size();i++)
						{
//						Statement exp=new ExpressionStmt();
//						exp(method.getBody().getStmts().get(i));
//						String type=method.getBody().getStmts().get(i).getClass().getTypeName();

// 						这里作为解析函数内成员变量的递归入口
							methodNodes.addAll(parsingMethodPartParameter(method.getBody().get().getStatements().get(i)));
						}
						methodClass.setMethodChildClass(methodNodes);
					}

					NodeList<Modifier> methodModifiers = method.getModifiers();
					for(Modifier modifier: methodModifiers) {
						Modifier.Keyword keyword = modifier.getKeyword();
						methodClass.setAllModifiers(keyword);
					}
					classMethodNode.add(methodClass);
					classNode.setMethod(classMethodNode);
				}
			}
			classNode.setNumberOfMethods(numberOfMethodsPerClass);
		}
		getClasses().add(classNode);
	}

	private void parsingInterface(ClassOrInterfaceDeclaration n) {
		numberOfInterfaces++;
		InterfaceNodeAST interfaceNode = new InterfaceNodeAST(getRepoURL(),
				n.getNameAsString(), getPackageFile());
		NodeList<Modifier> interfaceModifiers = n.getModifiers();
		for(Modifier modifier: interfaceModifiers) {
			Modifier.Keyword keyword = modifier.getKeyword();
			interfaceNode.setAllModifiers(keyword);
		}

		if (n.getAnnotations().size() != 0) {
			List<AnnotationNodeAST> interfAnn = new ArrayList<AnnotationNodeAST>();

			for (AnnotationExpr ann : n.getAnnotations()) {
				List<MemberValuePair> parameter = new ArrayList<MemberValuePair>();
				if(ann.getChildNodes().size()>1)
				{
					for(int i=1;i<ann.getChildNodes().size();i++)
					{
						if(ann.getChildNodes().get(i) instanceof MemberValuePair) {
							parameter.add((MemberValuePair) ann.getChildNodes().get(i));
						}
					}

				}
				AnnotationNodeAST annotation=new AnnotationNodeAST(ann.toString(),parameter);
				interfAnn.add(annotation);
				// logger.info("Implements interfaces: " + impl.getName());
			}
			interfaceNode.setAnnotatios(interfAnn);
		}

		if (n.getAllContainedComments().size() != 0) {
			List<CommentsNodeAST> interfComment = new ArrayList<CommentsNodeAST>();
			for (Comment comment : n.getAllContainedComments()) {
				interfComment.add(new CommentsNodeAST(comment.toString()));
			}
			interfaceNode.setComments(interfComment);
		}

		// Method parser of Interface
		if (n.getMembers().size() != 0) {
			long numberOfMethodsPerInterface = 0;
			List<InterfaceHasMethodNodeAST> interfMethodNode = new ArrayList<InterfaceHasMethodNodeAST>();
			for (BodyDeclaration member : n.getMembers()) {
				if (member instanceof MethodDeclaration) {
					numberOfMethodsPerInterface++;
					MethodDeclaration method = (MethodDeclaration) member;
					InterfaceHasMethodNodeAST methodInterface = new InterfaceHasMethodNodeAST(
							method.getNameAsString(), getPackageFile());
					methodInterface.setReturningType(method.getType()
							.toString());

					if (method.getAllContainedComments().size() != 0) {
						List<CommentsNodeAST> commentsMethod = new ArrayList<CommentsNodeAST>();
						for (Comment comment : method.getAllContainedComments()) {
							commentsMethod.add(new CommentsNodeAST(comment
									.toString()));
						}
						methodInterface.setComments(commentsMethod);
					}

					if (method.getAnnotations().size() != 0) {
						List<AnnotationNodeAST> annotatiosMethod = new ArrayList<AnnotationNodeAST>();


						for (AnnotationExpr ann : method.getAnnotations()) {
							List<MemberValuePair> parameter = new ArrayList<MemberValuePair>();
							if(ann.getChildNodes().size()>1)
							{
								for(int i=1;i<ann.getChildNodes().size();i++)
								{
									if(ann.getChildNodes().get(i) instanceof MemberValuePair) {
										parameter.add((MemberValuePair) ann.getChildNodes().get(i));
									}
								}

							}

							AnnotationNodeAST annotation=new AnnotationNodeAST(ann
									.toString(),parameter);
							annotatiosMethod.add(annotation);
						}
						methodInterface.setAnnotatios(annotatiosMethod);
					}

					if (method.getParameters().size() != 0) {
						List<ParameterMethodNodeAST> parametersMethod = new ArrayList<ParameterMethodNodeAST>();
						for (Parameter param : method.getParameters()) {
							parametersMethod.add(new ParameterMethodNodeAST(
									param.getType().toString(), param.getName()
											.toString()));
						}
						methodInterface.setParameters(parametersMethod);
					}

					if (method.getThrownExceptions().size() != 0) {
						List<ThrowMethodNodeAST> throwsMethod = new ArrayList<ThrowMethodNodeAST>();
						for (ReferenceType reftype : method.getThrownExceptions()) {
							throwsMethod.add(new ThrowMethodNodeAST(reftype
									.toString()));
						}
						methodInterface.setThrowsMethod(throwsMethod);
					}
					NodeList<Modifier> methodModifiers = method.getModifiers();
					for(Modifier modifier: methodModifiers) {
						Modifier.Keyword keyword = modifier.getKeyword();
						interfaceNode.setAllModifiers(keyword);
					}
					interfMethodNode.add(methodInterface);
					interfaceNode.setMethod(interfMethodNode);
				}
			}
			interfaceNode.setNumberOfMethods(numberOfMethodsPerInterface);
		}
		getInterfaces().add(interfaceNode);
	}

	public String getPackageFile() {
		return packageFile;
	}

	public long getNumberOfClasses() {
		return numberOfClasses;
	}

	public void setNumberOfClasses(long numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}

	public long getNumberOfInterfaces() {
		return numberOfInterfaces;
	}

	public void setNumberOfInterfaces(long numberOfInterfaces) {
		this.numberOfInterfaces = numberOfInterfaces;
	}

	public List<ClassNodeAST> getClasses() {
		return classes;
	}

	public void setClasses(List<ClassNodeAST> classes) {
		this.classes = classes;
	}

	public List<InterfaceNodeAST> getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(List<InterfaceNodeAST> interfaces) {
		this.interfaces = interfaces;
	}

	public String getRepoURL() {
		return repoURL;
	}

	public void setRepoURL(String repoURL) {
		this.repoURL = repoURL;
	}

	public List<String> getMethodPartParameters(){return methodPartParameters;}

	public void setMethodPartParameters(List<String> methodPartParameters){

			this.methodPartParameters=methodPartParameters;


	}

}
