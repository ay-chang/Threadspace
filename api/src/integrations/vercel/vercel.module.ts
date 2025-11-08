import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { VercelController } from './vercel.controller';
import { VercelService } from './vercel.service';

@Module({
    imports: [
        HttpModule.register({
            baseURL: 'https://api.vercel.com',
            timeout: 5000,
        }),
    ],
    controllers: [VercelController],
    providers: [VercelService],
})
export class VercelModule {}
