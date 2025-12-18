package edu.cnu.mdi.view.demo;

import java.awt.Toolkit;

import edu.cnu.mdi.graphics.toolbar.ToolActionButton;
import edu.cnu.mdi.graphics.toolbar.ToolContext;

@SuppressWarnings("serial")
public class BeepButton extends ToolActionButton {

    public BeepButton() {
        super("images/svg/bell.svg", "Beep");
    }

    @Override
    protected void perform(ToolContext ctx) {
        Toolkit.getDefaultToolkit().beep();
    }
}
