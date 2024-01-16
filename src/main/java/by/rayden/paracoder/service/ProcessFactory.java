package by.rayden.paracoder.service;

import org.springframework.stereotype.Service;

@Service
public class ProcessFactory {

    public ProcessBuilder createProcess(String command){
       return new ProcessBuilder().inheritIO().command(command);
    }
}
