import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { PrismaModule } from './prisma/prisma.module';
import { UsersModule } from './users/users.module';
import { VercelModule } from './integrations/vercel/vercel.module';
import { AppService } from './app.service';
import { AuthController } from './auth/auth.controller';
import { ProjectsModule } from './projects/projects.module';

@Module({
    imports: [PrismaModule, UsersModule, VercelModule, ProjectsModule],
    controllers: [AppController, AuthController],
    providers: [AppService],
})
export class AppModule {}
