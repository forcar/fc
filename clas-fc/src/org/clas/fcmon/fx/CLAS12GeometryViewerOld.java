/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.fcmon.fx;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javafx.application.Application;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;
import org.jlab.clasrec.utils.DataBaseLoader;
import org.jlab.detector.geant4.DCGeant4Factory;
import org.jlab.detector.geant4.FTOFGeant4Factory;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.geant.Geant4Basic;
import org.jlab.geom.geant.Geant4Mesh;
import org.jlab.geom.prim.Mesh3D;
import org.jlab.geom.prim.Transformation3D;

/**
 *
 * @author gavalian
 */
public class CLAS12GeometryViewerOld extends Application {
    
    
    
    ContentModel  content      = null;
    TreeView<String>  treeView = null;    
    Map<String,MeshStore>  meshStores = new TreeMap<String,MeshStore>();    
    BorderPane   mainBorderPane = null;       
    Group root = null;
    
    
    
    @Override
    public void start(Stage stage) throws Exception {
        
        this.mainBorderPane = new BorderPane();
        
        FlowPane  toolbar = new FlowPane();
        
        Button  btnClear = new Button("Clear");
        
        btnClear.setOnAction(event -> {root.getChildren().clear();});
        Button  btnLoadFtof = new Button("FTOF");
        
        btnLoadFtof.setOnAction(event -> {loadDetector("FTOF");});
        
        toolbar.getChildren().add(btnClear);
        toolbar.getChildren().add(btnLoadFtof);
        
        
        SplitPane  splitPane = new SplitPane();
        StackPane  treePane  = new StackPane();
        
        root = new Group();
        BorderPane pane=new BorderPane();
                
        treeView = new TreeView<String>();
        
        treePane.getChildren().add(treeView);
        
        this.content = new ContentModel(800,800,200);
        this.content.setContent(root);
        
        
        content.getSubScene().heightProperty().bind(pane.heightProperty());
        content.getSubScene().widthProperty().bind(pane.widthProperty());
        
        pane.setCenter(content.getSubScene());
        mainBorderPane.setTop(toolbar);
        splitPane.getItems().addAll(treePane,pane);
        splitPane.setDividerPositions(0.2);

        //this.addDetector("FTOF");
        //this.test();
        this.testFTOF();
        //this.testDC();
        //this.testBST();
        //final Scene scene = new Scene(pane, 880, 880, true);
        this.mainBorderPane.setCenter(splitPane);
        HBox statusPane = new HBox();
        
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setOnAction(new EventHandler() {
            @Override
            public void handle(Event event) {
                content.setBackgroundColor(colorPicker.getValue());
            }            
        });
        statusPane.getChildren().add(colorPicker);
        this.mainBorderPane.setBottom(statusPane);
        
        final Scene scene = new Scene(mainBorderPane, 1280, 880, true);
        
        scene.setFill(Color.ALICEBLUE);
        stage.setTitle("CLAS12 Geometry Viewer - JavaFX3D");
        stage.setScene(scene);
        stage.show();
    }
    
    public void testBST(){
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(new Color(0.1,0.1,0.8,0.5));
        mat.setSpecularColor(new Color(0.1,0.1,0.8,0.5));
        MeshStore  store = GeometryLoader.getGeometry("EC");
        for(Map.Entry<String,MeshView> item : store.getMap().entrySet()){
            item.getValue().setMaterial(mat);
            root.getChildren().add(item.getValue());
        }
    }

    public void testFTOF(){
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(new Color(0.1,0.1,0.8,0.5));
        mat.setSpecularColor(new Color(0.1,0.1,0.8,0.5));
        MeshStore  store = GeometryLoader.getGeometryGemc();
        for(Map.Entry<String,MeshView> item : store.getMap().entrySet()){
            //item.getValue().setMaterial(mat);
            root.getChildren().add(item.getValue());
        }
    }
    
    public void loadDetector(String detector){
        if(detector.compareTo("FTOF")==0){
            MeshStore  store = GeometryLoader.getGeometryGemc();
            for(Map.Entry<String,MeshView> item : store.getMap().entrySet()){
                //item.getValue().setMaterial(mat);
                root.getChildren().add(item.getValue());
            }
        }
        
    }
    
    public void testDC(){
        
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(new Color(0.2,0.2,0.4,0.4));
        mat.setSpecularColor(new Color(0.4,0.4,0.8,0.4));
        //mat.setSpecularPower(0.5);

//        DCGeant4Factory  factory = new DCGeant4Factory();
//        Geant4Basic  g4v   = factory.getRegion(1);
//        List<MeshView>  meshes = Geant4Mesh.getMesh(g4v);
//        for(MeshView mesh : meshes) mesh.setMaterial(mat);
//        root.getChildren().addAll(meshes);
//        System.out.println(" Meshes Size = " + meshes.size());
        /*
        
        Geant4Basic  dcLayer = factory.getLayer(100, 120, 1.0, 
                Math.toRadians(12.5), Math.toRadians(6.0));
        
        MeshView  mesh = Geant4Mesh.makeMeshTrap(dcLayer);
        this.root.getChildren().add(mesh);
        System.out.println(dcLayer);*/
        
    }
    
    public void test(){
        MeshStore store = new MeshStore();
        Mesh3D  box = Mesh3D.box(100, 25, 35);
        
        //Geant4Basic  shape = new Geant4Basic("","box",20,20,80);
        //MeshView mesh = Geant4Mesh.makeMeshBox(shape);
        //box.translateXYZ(40.0, 0.0, 120.0);
        box.rotateZ(Math.toRadians(30.0));
        MeshView mesh = box.getMeshView();
        mesh.setMaterial(store.getMaterials().get(2));
        this.root.getChildren().add(mesh);
    }
    
    public void addDetector(String name){
        
        TreeItem<String>  clasROOT = new TreeItem<String>("CLAS12");
        
        this.treeView.setEditable(true);
        this.treeView.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        for(int sector = 1; sector <=6 ; sector++){
            MeshStore  store = GeometryLoader.getGeometry("FTOF",sector,1);
            store.setMaterial(0.6, 0.2, 0.2, 0.4);
            this.meshStores.put(store.getName(), store);
            //treeView = new TreeView<String>();
            //this.treeView.setRoot(store.getMeshTree());
            clasROOT.getChildren().add(store.getMeshTree());
            for(Map.Entry<String,MeshView> entry : store.getMap().entrySet()){
                root.getChildren().add(entry.getValue());
            }
        }
        
        for(int sector = 1; sector <=6 ; sector++){
            MeshStore  store = GeometryLoader.getGeometry("FTOF",sector,2);
            store.setMaterial(0.1, 0.1, 0.4, 0.4);
            this.meshStores.put(store.getName(), store);
            //treeView = new TreeView<String>();
            clasROOT.getChildren().add(store.getMeshTree());
            for(Map.Entry<String,MeshView> entry : store.getMap().entrySet()){
                root.getChildren().add(entry.getValue());
            }
        }
        
        for(int sector = 1; sector <=6 ; sector++){
            MeshStore  store = GeometryLoader.getGeometry("FTOF",sector,3);
            //store.setVisible(false);
                    
            store.setMaterial(0.1, 0.4, 0.1, 0.4);
            this.meshStores.put(store.getName(), store);
            //treeView = new TreeView<String>();
            clasROOT.getChildren().add(store.getMeshTree());
            for(Map.Entry<String,MeshView> entry : store.getMap().entrySet()){
                root.getChildren().add(entry.getValue());
            }
        }
        
        this.treeView.setRoot(clasROOT);
        
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
