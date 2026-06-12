package com.minicrm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "${frontend.url}")
public class RootController {

    @Value("${frontend.url}")
    private String frontendUrl;

    @GetMapping(value = "/", produces = "text/html")
    public String index() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mini CRM - Backend API</title>
                <style>
                    body {
                        background-color: #030303;
                        color: #f4f4f5;
                        font-family: system-ui, -apple-system, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                    }
                    .card {
                        background-color: rgba(9, 9, 11, 0.6);
                        backdrop-filter: blur(10px);
                        border: 1px solid rgba(39, 39, 42, 0.8);
                        padding: 2.5rem;
                        border-radius: 1rem;
                        text-align: center;
                        max-width: 450px;
                        box-shadow: 0 0 50px rgba(99, 102, 241, 0.15);
                    }
                    .badge {
                        background-color: rgba(16, 185, 129, 0.1);
                        color: #34d399;
                        border: 1px solid rgba(16, 185, 129, 0.2);
                        padding: 0.25rem 0.75rem;
                        border-radius: 9999px;
                        font-size: 0.75rem;
                        font-weight: bold;
                        text-transform: uppercase;
                        display: inline-flex;
                        align-items: center;
                        gap: 0.5rem;
                        margin-bottom: 1.5rem;
                    }
                    .dot {
                        height: 6px;
                        width: 6px;
                        background-color: #10b981;
                        border-radius: 50%;
                        display: inline-block;
                        box-shadow: 0 0 8px #10b981;
                    }
                    h1 {
                        font-size: 1.75rem;
                        font-weight: 800;
                        margin: 0 0 0.5rem 0;
                        background: linear-gradient(135deg, #818cf8, #c084fc);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                    }
                    p {
                        color: #a1a1aa;
                        font-size: 0.875rem;
                        margin-bottom: 2rem;
                        line-height: 1.5;
                    }
                    .btn {
                        display: inline-block;
                        background-color: #4f46e5;
                        color: white;
                        text-decoration: none;
                        padding: 0.75rem 1.5rem;
                        border-radius: 0.5rem;
                        font-weight: 600;
                        font-size: 0.875rem;
                        transition: all 0.2s;
                        box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
                    }
                    .btn:hover {
                        background-color: #4338ca;
                        transform: translateY(-1px);
                        box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4);
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="badge">
                        <span class="dot"></span>
                        Backend API Online
                    </div>
                    <h1>Mini CRM Engine</h1>
                    <p>The Spring Boot backend microservice is running successfully on port 8080 in dual memory-persistence mode.</p>
                    <a href="%s" class="btn">Launch CRM Frontend Console</a>
                </div>
            </body>
            </html>
            """.formatted(frontendUrl);
    }
}
