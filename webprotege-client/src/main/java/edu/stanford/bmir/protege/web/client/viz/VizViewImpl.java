package edu.stanford.bmir.protege.web.client.viz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.stanford.bmir.protege.web.client.JSON;
import edu.stanford.bmir.protege.web.client.action.UIAction;
import edu.stanford.bmir.protege.web.client.d3.*;
import edu.stanford.bmir.protege.web.client.graphlib.Graph;
import edu.stanford.bmir.protege.web.client.graphlib.Graph2Svg;
import edu.stanford.bmir.protege.web.client.graphlib.NodeDetails;
import edu.stanford.bmir.protege.web.client.library.popupmenu.PopupMenu;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.events.Event;
import elemental.events.MouseEvent;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 11 Oct 2018
 */
public class VizViewImpl extends Composite implements VizView {

    private static final double ZOOM_DELTA = 0.05;

    private static VizViewImplUiBinder ourUiBinder = GWT.create(VizViewImplUiBinder.class);

    private final PopupMenu popupMenu;

    private final PopupPanel popupPanel = new PopupPanel();

    @UiField
    HTMLPanel canvas;

    @UiField
    ListBox ranksepListBox;

    @UiField
    Button downloadButton;

    @UiField
    TextMeasurerImpl textMeasurer;

    @Nonnull
    private Runnable loadHandler = () -> {
    };

    private DownloadHandler downloadHandler = () -> {
    };

    private Consumer<NodeDetails> nodeClickHandler = n -> {
    };

    private Consumer<NodeDetails> nodeDoubleClickHandler = n -> {
    };

    private Consumer<NodeDetails> nodeContextClickHandler = n -> {
    };

    private double scaleFactor = 1.0;

    private SettingsChangedHandler settingsChangedHandler = () -> {};

    private Optional<NodeDetails> mostRecentTargetNode = Optional.empty();

    private BiConsumer<NodeDetails, Event> nodeMouseOverHandler = (n, e) -> {};

    @Inject
    public VizViewImpl() {
        popupMenu = new PopupMenu();
        initWidget(ourUiBinder.createAndBindUi(this));
        ranksepListBox.setSelectedIndex(1);
        ranksepListBox.addChangeHandler(event -> settingsChangedHandler.handleSettingsChanged());
    }

    @Override
    public void setNodeClickHandler(@Nonnull Consumer<NodeDetails> nodeClickHandler) {
        this.nodeClickHandler = checkNotNull(nodeClickHandler);
    }

    @Override
    public void setNodeDoubleClickHandler(@Nonnull Consumer<NodeDetails> nodeDoubleClickHandler) {
        this.nodeDoubleClickHandler = checkNotNull(nodeDoubleClickHandler);
    }

    @Override
    public void setNodeContextMenuClickHandler(@Nonnull Consumer<NodeDetails> nodeContextMenuClickHandler) {
        this.nodeContextClickHandler = checkNotNull(nodeContextMenuClickHandler);
    }

    @Override
    public void setNodeMouseOverHandler(BiConsumer<NodeDetails, Event> nodeMouseOverHandler) {
        this.nodeMouseOverHandler = checkNotNull(nodeMouseOverHandler);
    }

    @Override
    public void addContextMenuAction(@Nonnull UIAction uiAction) {
        popupMenu.addItem(uiAction);
    }

    private void handleKeyDown(KeyDownEvent event) {

    }

    @Override
    public void setLoadHandler(Runnable handler) {
        this.loadHandler = checkNotNull(handler);
    }

    @Override
    public void setDownloadHandler(@Nonnull DownloadHandler handler) {
        this.downloadHandler = checkNotNull(handler);
    }

    @Nonnull
    @Override
    public TextMeasurer getTextMeasurer() {
        return textMeasurer;
    }

    @Override
    public Element getSvgElement() {
        return (Element) canvas.getElement().getElementsByTagName("svg").getItem(0);
    }

    @Override
    public void clearGraph() {
        GWT.log("[VizViewImpl] clear graph");
        removeCanvasChildren();
    }

    @Override
    public Optional<NodeDetails> getMostRecentTargetNode() {
        return mostRecentTargetNode;
    }

    @Override
    public void setGraph(Graph graph) {
        removeCanvasChildren();
        Graph2Svg graph2Svg = new Graph2Svg(textMeasurer, graph);
        graph2Svg.setNodeClickHandler(this::handleNodeClick);
        graph2Svg.setNodeDoubleClickHandler(this::handleNodeDoubleClick);
        graph2Svg.setNodeContextMenuClickHandler(this::handleNodeContextMenuClick);
        graph2Svg.setNodeMouseOverHandler(this::handleNodeMouseOver);
        Element svg = graph2Svg.createSvg();
        Element element = (Element) canvas.getElement();
        element.appendChild(svg);
        Selection svgElement = d3.selectElement(element).select("svg");
        Selection gElement = svgElement.select("g");
        Zoom zoom = d3.zoom();
        Object zoomFunc = zoom.on("zoom", () -> {
            Transform transform = d3.getEvent().getTransform();
            gElement.attr("transform", "translate(" + transform.getX() + " " + transform.getY() + ")" + " scale(" + transform.getK() + ")");
        });
        svgElement.call(zoomFunc);
    }

    private void handleNodeClick(NodeDetails n, Event e) {
        mostRecentTargetNode = Optional.of(n);
        nodeClickHandler.accept(n);
    }

    private void handleNodeMouseOver(NodeDetails n, Event e) {
        mostRecentTargetNode = Optional.of(n);
        nodeMouseOverHandler.accept(n, e);
    }

    private void handleNodeContextMenuClick(NodeDetails n, Event e) {
        e.preventDefault();
        e.stopPropagation();
        mostRecentTargetNode = Optional.of(n);
        handleContextMenu(n, e);
        nodeContextClickHandler.accept(n);
    }

    private void handleNodeDoubleClick(NodeDetails n, Event e) {
        mostRecentTargetNode = Optional.of(n);
        nodeDoubleClickHandler.accept(n);
    }

    private void handleContextMenu(@Nonnull NodeDetails nodeDetails, @Nonnull Event event) {
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            popupMenu.show(mouseEvent.getClientX(),
                           mouseEvent.getY());
        }
    }

    private void removeCanvasChildren() {
        GWT.log("[VizViewImpl] remove canvas children");
        Element canvasElement = (Element) canvas.getElement();
        NodeList childNodes = canvasElement.getChildNodes();
        while (childNodes.getLength() > 0) {
            canvasElement.removeChild(childNodes.item(0));
        }
    }

    @Override
    public double getRankSpacing() {
        String value = ranksepListBox.getSelectedValue();
        try {
            return 2 * Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 2.0;
        }
    }

    @Override
    public void setSettingsChangedHandler(@Nonnull SettingsChangedHandler handler) {
        this.settingsChangedHandler = checkNotNull(handler);
    }

    @UiHandler("downloadButton")
    public void downloadButtonClick(ClickEvent event) {
        Element e = (Element) canvas.getElement().getElementsByTagName("svg").getItem(0);
        downloadHandler.handleDownload();
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        loadHandler.run();

    }

    interface VizViewImplUiBinder extends UiBinder<HTMLPanel, VizViewImpl> {

    }
}