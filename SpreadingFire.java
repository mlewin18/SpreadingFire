import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.Random;

public class SpreadingFire extends Application{
    //initialize member vars
    Random random = new Random();
    final int BURNT = 0;
    final int LIT = 1;
    final int GRASS = 2;
    final int TREE = 3;
    final int BURNING = 4;
    final int SMOULDERING = 5;
    int cellsAcross = 300;
    double sceneSize = 750;
    double menuHeight = 30;
    double probTree = 0.007; // likelihood that a tree seed appears
    double treeSpread = .489; // likelihood that trees spread (set to zero to see randomization without proximity checking)
    double chanceStopsBurning = .05;
    double grassBurningModifier = .0;
    double treeBurningModifier = .5;
    double chanceFireTree = .02;
    double chanceFireGrass = .75;
    double cellSize = sceneSize/cellsAcross;
    ArrayList<Pair<Integer,Integer>> toFire = new ArrayList<>();
    ArrayList<Pair<Integer,Integer>> toBurning = new ArrayList<>();
    ArrayList<Pair<Integer,Integer>> toGoingOut = new ArrayList<>();
    Canvas canvas = new Canvas(sceneSize, sceneSize);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    //create cell int array
    //0 = Empty/Burnt, 1 = On Fire (First time), 2 = Regular Forest, 3 = Tree Forest, 4 = On Fire (Second time)
    int[][] cells = new int[cellsAcross][cellsAcross];
    int[][] origCells = new int[cellsAcross][cellsAcross];
    int frameCount = 0;
    int cutRate = 5; //factor of 60, by which the frame rate will get divided to slow animation
    AnimationTimer animator = new AnimationTimer() {
        public void handle( long time ) {
            if (frameCount%cutRate==0){
                change();
            }
            frameCount++;
        }
    };

    public static void main(String[] args) {
        launch(args);
    }
    public void start(Stage stage){
        //create scene and menu
        BorderPane border = new BorderPane();
        Button start = new Button("Start");
        start.setOnAction(e->animator.start());
        Button pause = new Button("Pause");
        pause.setOnAction(e->animator.stop());
        Button restart = new Button("Restart");
        restart.setOnAction(e->start(stage));
        HBox menu = new HBox(start, pause, restart);
        start.setPrefWidth(sceneSize/3);
        pause.setPrefWidth(sceneSize/3);
        restart.setPrefWidth(sceneSize/3);
        start.setPrefHeight(menuHeight);
        pause.setPrefHeight(menuHeight);
        restart.setPrefHeight(menuHeight);
        border.setBottom(menu);
        Scene scene = new Scene(border, sceneSize, sceneSize+menuHeight);

        //mouse event for setting fire
        border.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                double x = mouseEvent.getSceneX();
                double y = mouseEvent.getSceneY();
                int col = (int)(x/cellSize);
                int row = (int)(y/cellSize);
                cells[row][col]=1;
                drawCell(row, col, 1);
            }
        });

        //create canvas and populate seed trees
        gc.setFill(Color.BLUE);
        gc.fillRect(0,0,sceneSize, sceneSize-menuHeight);
        border.getChildren().add(canvas);

        for (int i=0; i<cellsAcross; i++) { //i is row
            for (int j = 0; j < cellsAcross; j++) { //j is col
                double ran = random.nextDouble();
                if (ran < probTree) {
                    cells[i][j] = TREE;
                } else {
                    cells[i][j] = GRASS;
                }
            }
        }

        //review canvas and expand forests
        int xAdd = 0;
        int yAdd = 0;
        int xStart = 10;
        int yStart = 10;
        int xCap = 0;
        int yCap = 0;
        for (int i=0; i<4; i++) {
            if(i == 0) {
                xStart = 0;
                xCap = cellsAcross;
                xAdd = 1;
                yStart = 0;
                yCap = cellsAcross;
                yAdd = 1;
            }
            else if(i == 1) {
                xStart = cellsAcross;
                xCap = 0;
                xAdd = -1;
                yStart = 0;
                yCap = cellsAcross;
                yAdd = 1;
            }
            else if(i == 2) {
                xStart = 0;
                xCap = cellsAcross;
                xAdd = 1;
                yStart = cellsAcross;
                yCap = 0;
                yAdd = -1;
            }
            else {
                xStart = cellsAcross;
                xCap = 0;
                xAdd = -1;
                yStart = cellsAcross;
                yCap = 0;
                yAdd = -1;
            }
            for (int x = xStart; x < xCap; x+=xAdd) { //x is row
                for (int y = yStart; y < yCap; y+=yAdd) { //y is col
                    int surroundingDensity = totalNextTo(3, x, y);
                    if (surroundingDensity != 0) {
                        double randSpread = random.nextDouble() - (surroundingDensity * treeSpread);
                        if (randSpread < probTree) {
                            cells[x][y] = 3;
                        } else {
                            cells[x][y] = 2;
                        }
                    }
                }
            }
        }
        origCells = cells.clone();


        //create initial forest squares
        for (int i=0; i<cellsAcross; i++){ //i is row
            for (int j=0; j<cellsAcross; j++){ //j is col
                if (cells[i][j] == GRASS){
                    gc.setFill(Color.GREEN);
                } else if (cells[i][j] == TREE){
                    gc.setFill(Color.DARKGREEN);
                } else {
                    gc.setFill(Color.RED);
                }
                gc.fillRect(cellSize*j, cellSize*i, cellSize, cellSize);
            }
        }

        //show scene
        stage.setScene(scene);
        stage.setTitle("Spreading Fire");
        stage.show();

    }

    //what happens each frame
    public void change(){
        for (int i=0; i<cellsAcross; i++){ //i is row
            for (int j=0; j<cellsAcross; j++){ //j is col
                if (cells[i][j] == LIT){
                    toBurning.add(new Pair<Integer, Integer>(i, j));
                } else if (cells[i][j]==GRASS){
                    if (nextTo(BURNING, i, j) && totalNextTo(GRASS, i, j) < 2){
                        toBurning.add(new Pair<Integer, Integer>(i, j));
                    }
                    else if(nextTo(LIT, i, j)){
                        if(isFire(GRASS)){
                            toFire.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                } else if (cells[i][j]==TREE){
                    if(nextTo(LIT, i, j) || nextTo(BURNING, i, j)) {
                        if (isFire(TREE)) {
                            toFire.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                } else if (cells[i][j]==BURNING){
                    if (origCells[i][j] == TREE) {
                        if (stayBurning(BURNING, treeBurningModifier)) {
                            toGoingOut.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                    else {
                        if (stayBurning(BURNING, grassBurningModifier)) {
                            toGoingOut.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                } else if (cells[i][j]==SMOULDERING){
                    cells[i][j]=BURNT;
                    drawCell(i, j, BURNT);
                }
            }
        }
        for (Pair cell: toFire){
            int i = (Integer) cell.getKey();
            int j = (Integer) cell.getValue();
            cells[i][j] = LIT;
            drawCell(i, j, LIT);
        }
        toFire.clear();

        for (Pair cell: toBurning){
            int i = (Integer) cell.getKey();
            int j = (Integer) cell.getValue();
            cells[i][j] = BURNING;
            drawCell(i, j, BURNING);
        }
        toBurning.clear();

        for (Pair cell: toGoingOut){
            int i = (Integer) cell.getKey();
            int j = (Integer) cell.getValue();
            cells[i][j] = SMOULDERING;
            drawCell(i, j, SMOULDERING);
        }
        toGoingOut.clear();

    }

    public void drawCell(int row, int col, int state){
        if (state==BURNT){
            gc.setFill(Color.GREY);
        } else if (state==LIT){
            gc.setFill(Color.ORANGE);
        } else if (state==GRASS){
            gc.setFill(Color.GREEN);
        } else if (state==TREE){
            gc.setFill(Color.BLACK);
        } else if (state==BURNING){
            gc.setFill(Color.RED);
        } else if (state==SMOULDERING){
            gc.setFill(Color.RED);
        }
        gc.fillRect(cellSize*col, cellSize*row, cellSize, cellSize);
    }

    public boolean isFire(int state){ // Computes fire spread
        double num = random.nextDouble();
        if (state==GRASS){
            if(num<chanceFireGrass){
                return true;
            } else {
                return false;
            }
        }
        else if(state==TREE){
            if(num<chanceFireTree){
                return true;
            } else {
                return false;
            }
        } else{
            return false;
        }
    }
    public boolean stayBurning(int status, double modify){ // Computes fire spread
        double num = random.nextDouble() + modify;
        if(num<chanceStopsBurning){
            return true;
        } else {
            return false;
        }
    }
    //checks if surrounding cells are the given state
    public boolean nextTo(int state, int row, int col){
        if (row == 0 && col==0){
            return cells[row + 1][col] == state || cells[row][col + 1] == state;
        } else if (row == 0 && col == cellsAcross-1){
            return cells[row + 1][col] == state || cells[row][col - 1] == state;
        } else if (row == cellsAcross-1 && col == cellsAcross-1){
            return cells[row - 1][col] == state || cells[row][col - 1] == state;
        } else if (row == cellsAcross-1 && col==0){
            return cells[row - 1][col] == state || cells[row][col + 1] == state;
        } else if (row==0){
            return cells[row + 1][col] == state || cells[row][col - 1] == state || cells[row][col+1] == state;
        } else if (col==0){
            return cells[row + 1][col] == state || cells[row-1][col] == state || cells[row][col+1] == state;
        } else if (row ==cellsAcross-1){
            return cells[row - 1][col] == state || cells[row][col - 1] == state || cells[row][col+1] == state;
        } else if (col == cellsAcross-1){
            return cells[row + 1][col] == state || cells[row][col - 1] == state || cells[row-1][col] == state;
        } else {
            return cells[row + 1][col] == state || cells[row][col - 1] == state || cells[row][col+1] == state||cells[row-1][col] == state;
        }
    }
    //checks how many of a given type are nearby and returns the total
    public int totalNextTo(int state, int row, int col){
        int totalSurrounding = 0;
        if (row == 0 && col== 0){ // upper right corner of screen
            if (cells[row + 1][col] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col + 1] == state){
                totalSurrounding += 1;
            }
        } else if (row==0&&col==cellsAcross-1){ // upper left corner of screen
            if(cells[row + 1][col] == state){
                totalSurrounding += 1;
            }
            if(cells[row][col - 1] == state){
                totalSurrounding += 1;
            }
        } else if (row==cellsAcross-1&&col==cellsAcross-1){ // bottom left corner of screen
            if (cells[row - 1][col] == state) {
                totalSurrounding += 1;
            }
            if (cells[row][col - 1] == state) {
                totalSurrounding += 1;
            }
        } else if (row==cellsAcross-1&&col==0){ // bottom right corner of screen
            if (cells[row - 1][col] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col + 1] == state){
                totalSurrounding += 1;
            }
        } else if (row==0){ // top of screen
            if (cells[row + 1][col] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col - 1] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col+1] == state){
                totalSurrounding += 1;
            }
        } else if (col==0){ // left of screen
            if (cells[row + 1][col] == state){
                totalSurrounding += 1;
            }
            if (cells[row-1][col] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col+1] == state){
                totalSurrounding += 1;
            }
        } else if (row ==cellsAcross-1){ // bottom of screen
            if (cells[row - 1][col] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col - 1] == state){
                totalSurrounding += 1;
            }
            if (cells[row][col+1] == state){
                totalSurrounding += 1;
            }
        } else if (col == cellsAcross-1){ // right of screen
            if (cells[row + 1][col] == state) {
                totalSurrounding += 1;
            }
            if (cells[row][col - 1] == state) {
                totalSurrounding += 1;
            }
            if (cells[row-1][col] == state) {
                totalSurrounding += 1;
            }
        } else {

            if (cells[row + 1][col] == state) { // below cell
                totalSurrounding += 1;
            }if (cells[row][col + 1] == state) { // right of cell
                totalSurrounding += 1;
            }if (cells[row - 1][col] == state) { // above cell
                totalSurrounding += 1;
            }if (cells[row][col - 1] == state) { // left of cell
                totalSurrounding += 1;
            }
        }return totalSurrounding;
    }



}