package com.samvbeckmann.parity;

import com.samvbeckmann.parity.model.AgentModel;
import com.samvbeckmann.parity.model.CommunityModel;
import com.samvbeckmann.parity.model.CommunityNode;
import com.samvbeckmann.parity.reference.Names;
import com.samvbeckmann.parity.reference.Reference;
import com.samvbeckmann.parity.view.AgentAddDialogController;
import com.samvbeckmann.parity.view.CommunityViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Initializes the GUI portion of Parity.
 *
 * @author Sam Beckmann & Nate Beckemeyer
 */
public class MainApp extends Application
{
    private Stage primaryStage;
    private BorderPane rootLayout;
    private CommunityViewController communityController;

    private static CommunityModel activeCommunity = new CommunityModel();

    public MainApp()
    {
        // NOOP
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    public CommunityModel getActiveCommunity()
    {
        return activeCommunity;
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        ParityRegistry.initializeRegistry();

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(Reference.NAME);
        this.primaryStage.setMinWidth(600);
        this.primaryStage.setMinHeight(500);

        initRootLayout();
        showConfigurationSettings();
        showCommunityOverview();
    }

    /**
     * Initializes the root layout.
     */
    public void initRootLayout()
    {
        try
        {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource(Names.FXMLPaths.ROOT_LAYOUT));
            rootLayout = loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void showCommunityOverview()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource(Names.FXMLPaths.AGENT_VIEW));
            BorderPane communityView = loader.load();

            SplitPane split = (SplitPane) rootLayout.getCenter();
            split.getItems().add(communityView);

            CommunityViewController controller = loader.getController();
            controller.setMainApp(this);
            this.communityController = controller;
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void showConfigurationSettings()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource(Names.FXMLPaths.CONFIGURATION_VIEW));
            BorderPane configurationSettings = loader.load();

            GridPane grid = (GridPane) ((VBox) configurationSettings.getTop()).getChildren().get(2);

            ComboBox<ReflectionWrapper> interactionHandlers = (ComboBox<ReflectionWrapper>) grid.getChildren().get(1);
            ComboBox<ReflectionWrapper> completionConditions = (ComboBox<ReflectionWrapper>) grid.getChildren().get(3);

            interactionHandlers.setItems(ParityRegistry.getInteractionHandlers());
            completionConditions.setItems(ParityRegistry.getCompletionConditions());

            AnchorPane pane = new AnchorPane();
            configurationSettings.setCenter(pane);
            pane.getChildren().add(createDraggableCircle(50, 50));

            SplitPane split = (SplitPane) rootLayout.getCenter();
            split.getItems().add(configurationSettings);

            pane.getChildren().add(createDraggableCircle(pane.getWidth() / 2, pane.getHeight() / 2));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean showAgentAddDialog(List<AgentModel> newAgents)
    {
        try
        {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource(Names.FXMLPaths.AGENT_ADD_DIALOGUE));
            AnchorPane page = loader.load();

            // Create the dialogue Stage.
            Stage dialogueStage = new Stage();
            dialogueStage.setTitle("Add Agents");
            dialogueStage.initModality(Modality.WINDOW_MODAL);
            dialogueStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogueStage.setScene(scene);

            // Set the person into the controller.
            AgentAddDialogController controller = loader.getController();
            controller.setDialogStage(dialogueStage);
            controller.setList(newAgents);

            // Show the dialog and wait until the user closes it
            dialogueStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private Circle createDraggableCircle(double x, double y)
    {
        final double circleRadius = 20;

        CommunityNode circle = new CommunityNode(x, y, circleRadius);
        circle.setFill(Color.DEEPSKYBLUE);
        Wrapper<Point2D> mouseLocation = new Wrapper<>();

        circle.setOnDragDetected(event ->
        {
            circle.getParent().setCursor(Cursor.CLOSED_HAND);
            mouseLocation.value = new Point2D(event.getSceneX(), event.getSceneY());
        });

        circle.setOnMouseReleased(event ->
        {
            circle.getParent().setCursor(Cursor.DEFAULT);
            mouseLocation.value = null;
        });

        circle.setOnMouseDragged(event ->
        {
            if (mouseLocation.value != null)
            {
                double deltaX = event.getSceneX() - mouseLocation.value.getX();
                double deltaY = event.getSceneY() - mouseLocation.value.getY();
                double newX = circle.getCenterX() + deltaX;
                double newY = circle.getCenterY() + deltaY;
                circle.setCenterX(newX);
                circle.setCenterY(newY);
                mouseLocation.value = new Point2D(event.getSceneX(), event.getSceneY());
            }
        });

        circle.setOnContextMenuRequested(event ->
                circle.setFill(Color.BLUE));

        circle.setOnMouseClicked(event ->
        {
            if (event.getButton().equals(MouseButton.PRIMARY))
            {
                activeCommunity = circle.getCommunity();
                communityController.updateCommunity();
            }
        });

        return circle;
    }

    /**
     * @return The main stage.
     */
    public Stage getPrimaryStage()
    {
        return primaryStage;
    }

    static class Wrapper<T>
    {
        T value;
    }
}
