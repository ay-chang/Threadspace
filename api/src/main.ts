import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
    const app = await NestFactory.create(AppModule);

    // Enable CORS so Next.js frontend (localhost:3000) can talk to Nest
    app.enableCors({
        origin: ['http://localhost:3000'], // frontend URL(s)
        methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
        allowedHeaders: ['Content-Type', 'Authorization', 'x-internal-token'],
        credentials: false, // set to true if we ever send cookies
    });

    // Use port 8000 and send messages
    await app.listen(process.env.PORT ?? 8080);
    console.log(
        `Nest server running on http://localhost:${process.env.PORT ?? 8080}`,
    );
}
bootstrap();
