package com.elasticthree.ASTCreator.ASTCreator;

import com.elasticthree.ASTCreator.ASTCreator.Helpers.RecursivelyProjectJavaFiles;
import com.elasticthree.ASTCreator.ASTCreator.Neo4jDriver.Neo4JDriver;
import com.elasticthree.ASTCreator.ASTCreator.Neo4jDriver.Neo4JInsertClassRelation;
import com.elasticthree.ASTCreator.ASTCreator.Objects.ClassNodeAST;
import com.elasticthree.ASTCreator.ASTCreator.Objects.FileNodeAST;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.text.html.Option;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ASTAnalyse {

    static final Logger stdoutLog = Logger.getLogger(ASTCreator.class);
    static final Logger debugLog = Logger.getLogger("debugLogger");

    private String repoURL;
    private long javaLinesOfCode;

    public ASTAnalyse(String repoURL) {
        setRepoURL(repoURL);
        setJavaLinesOfCode(0);
    }

    /**
     * We use CompilationUnit (from Javaparser project) to parse the File
     *
     * @param path_to_class
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public CompilationUnit getClassCompilationUnit(String path_to_class) {
        // creates an input stream for the file to be parsed
        FileInputStream in = null;
        CompilationUnit cu = null;
        try {
            in = new FileInputStream(path_to_class);
        } catch (FileNotFoundException e) {
            debugLog.debug("IO Error skip project " + path_to_class
                    + " from AST - Graph procedure");
            return cu;
        }
        try {
            JavaParser javaParser = new JavaParser();
            cu = javaParser.parse(in).getResult().get();
//			cu = JavaParser.parse(in);
        } catch (Exception e1) {
            debugLog.debug("Parsing Error skip project " + path_to_class
                    + " from AST - Graph procedure");
        }
        try {
            in.close();
        } catch (IOException e) {
            debugLog.debug("IO Error skip project " + path_to_class
                    + " from AST - Graph procedure");
        }
        return cu;
    }

    /**
     * This function creates AST (Abstract syntax tree) for a Java file
     *
     * @param path_to_class
     * @return
     */
    public void getASTFileObject(String path_to_class) {

        FileNodeAST fileObject = null;
        CompilationUnit cu;
        cu = getClassCompilationUnit(path_to_class);
        String className = path_to_class.split("\\/|\\.")[path_to_class.split("\\/|\\.").length - 2];
        Optional<ClassOrInterfaceDeclaration> ci = cu.getClassByName(className);
        Optional<TokenRange> tokens = ci.get().getTokenRange();
        String content = tokens.get().toString();
        System.out.println(content);
    }

    /**
     * This function runs the AST and inserts it in Neo4j instance for all Java
     * files of a Java Project
     *
     * @param classes
     */
    public void repoASTProcedure(List<String> classes) {
        classes.forEach(file -> {
            stdoutLog.info("-> Java File: " + file);
            getASTFileObject(file);
            System.out.println("done");
        });

    }

    public static void main(String[] args) {
        // args[0] -> Path to Java Project
        List<String> classes = RecursivelyProjectJavaFiles
                .getProjectJavaFiles("/Users/yangchen/Documents/Papers/dataset_projects/microservices-platform-master");
        // args[1] -> URL of Java Project
        ASTAnalyse ast = new ASTAnalyse("microservices-platform-master");
        ast.repoASTProcedure(classes);
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String repoURL) {
        this.repoURL = repoURL;
    }

    public long getJavaLinesOfCode() {
        return javaLinesOfCode;
    }

    public void setJavaLinesOfCode(long javaLinesOfCode) {
        this.javaLinesOfCode = javaLinesOfCode;
    }

    public void addJavaLinesOfCode(long javaLinesOfCode) {
        this.javaLinesOfCode += javaLinesOfCode;
    }

}

