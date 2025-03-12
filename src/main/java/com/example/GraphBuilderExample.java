package com.example;

import org.jgrapht.Graph;
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
            Graph<Stop, RouteEdge> graph = graphBuilderService.buildGraph();
            System.out.println("Graf Kenarları (Yönsüz):");
            graph.edgeSet().forEach(edge -> {
                Stop source = graph.getEdgeSource(edge);
                Stop target = graph.getEdgeTarget(edge);
                System.out.println(source.getId() + " - " + target.getId() + " | " + edge);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
