package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GraphBuilderExample implements CommandLineRunner {
    private final GraphBuilderService graphBuilderService;

    @Autowired
    public GraphBuilderExample(GraphBuilderService graphBuilderService) {
        this.graphBuilderService = graphBuilderService;
    }

    public static void main(String[] args) {
        SpringApplication.run(GraphBuilderExample.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            ManualGraph graph = graphBuilderService.buildGraph();
            System.out.println("Graf Kenarları (Yönsüz):");
            
            for (Stop stop : graph.getVertices()) {
                for (EdgeInfo edgeInfo : graph.getEdges(stop)) {
                    if (edgeInfo.getFrom().equals(stop)) {
                        Stop source = edgeInfo.getFrom();
                        Stop target = edgeInfo.getTo();
                        System.out.println(source.getId() + " - " + target.getId() + " | " + edgeInfo.getRouteEdge());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
