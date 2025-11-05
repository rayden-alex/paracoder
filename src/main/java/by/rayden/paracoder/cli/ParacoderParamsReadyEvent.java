package by.rayden.paracoder.cli;

import org.springframework.context.ApplicationEvent;

/**
 * Custom event (synchronous) to notify subscribers that command line parameters were successfully parsed
 */
public class ParacoderParamsReadyEvent extends ApplicationEvent {

    public ParacoderParamsReadyEvent(Object source) {
        super(source);
    }
}
