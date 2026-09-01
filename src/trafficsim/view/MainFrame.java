package trafficsim.view;

import trafficsim.command.PauseCommand;
import trafficsim.command.ResetCommand;
import trafficsim.command.ResumeCommand;
import trafficsim.command.SetTickRateCommand;
import trafficsim.command.SpawnOneCommand;
import trafficsim.command.StepOnceCommand;
import trafficsim.engine.SimulationEngine;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame {

    private final SimulationEngine engine;
    private final SimulationDisplay display;

    public MainFrame(SimulationEngine engine) {
        super("TrafficSim — bird's-eye");
        this.engine = engine;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        display = new SimulationDisplay(engine);
        engine.addObserver(display);
        add(display, BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);

        installKeyBindings();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildControls() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        bar.setBackground(new Color(0x28, 0x2C, 0x34));

        JButton pause  = button("Pause",  () -> engine.submit(new PauseCommand()));
        JButton resume = button("Resume", () -> engine.submit(new ResumeCommand()));
        JButton step   = button("Step",   () -> engine.submit(new StepOnceCommand()));
        JButton reset  = button("Reset",  () -> engine.submit(new ResetCommand()));
        JButton spawn  = button("Spawn 1", () -> engine.submit(new SpawnOneCommand()));

        JLabel speedLbl = new JLabel("  Speed:");
        speedLbl.setForeground(Color.WHITE);

        JSlider speed = new JSlider(1, 120, engine.getTickRate());
        speed.setMajorTickSpacing(20);
        speed.setPaintTicks(true);
        speed.setOpaque(false);
        speed.setForeground(Color.WHITE);
        speed.addChangeListener(e -> engine.submit(new SetTickRateCommand(speed.getValue())));

        JCheckBox highlightEV = new JCheckBox("Highlight EV");
        highlightEV.setForeground(Color.WHITE);
        highlightEV.setOpaque(false);
        highlightEV.addActionListener(e -> display.setHighlightEmergency(highlightEV.isSelected()));

        JCheckBox showOverlay = new JCheckBox("Congestion overlay");
        showOverlay.setForeground(Color.WHITE);
        showOverlay.setOpaque(false);
        showOverlay.addActionListener(e -> display.setShowCongestionOverlay(showOverlay.isSelected()));

        bar.add(pause); bar.add(resume); bar.add(step); bar.add(reset); bar.add(spawn);
        bar.add(speedLbl); bar.add(speed);
        bar.add(highlightEV); bar.add(showOverlay);
        return bar;
    }

    private void installKeyBindings() {
        JComponent root = getRootPane();
        bind(root, KeyStroke.getKeyStroke(' '), "toggle-pause", e -> {
            engine.submit(engine.isPaused() ? new ResumeCommand() : new PauseCommand());
        });
        bind(root, KeyStroke.getKeyStroke('s'), "step-once", e -> engine.submit(new StepOnceCommand()));
        bind(root, KeyStroke.getKeyStroke('n'), "spawn-one", e -> engine.submit(new SpawnOneCommand()));
        bind(root, KeyStroke.getKeyStroke('r'), "reset",     e -> engine.submit(new ResetCommand()));
    }

    private static void bind(JComponent c, KeyStroke ks, String name, java.util.function.Consumer<ActionEvent> action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.accept(e); }
        });
    }

    private static JButton button(String label, Runnable action) {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.addActionListener(e -> action.run());
        return b;
    }
}
