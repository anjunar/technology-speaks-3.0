import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { resolve } from "node:path"

export default defineConfig({
    root: "frontend/app/src/main/webapp/",
    plugins: [scalaJSPlugin()],
    resolve: {
        alias: {
            "@jfx-css": resolve(__dirname, "./frontend/jfx/src/main/resources/jfx/index.css")
        }
    },
    server: {
        proxy: {
            "/service": {
                target: "http://localhost:8080",
                changeOrigin: true
            }
        }
    }
});