import { Injectable } from '@nestjs/common';
import { PrismaService } from 'src/prisma/prisma.service';
import { CreateProjectDto } from './dtos/create-project.dto';

@Injectable()
export class ProjectsService {
    constructor(private prismaService: PrismaService) {}

    create(dto: CreateProjectDto) {
        return this.prismaService.project.create({
            data: {
                userId: dto.userId,
                name: dto.userId,
                description: dto.description ?? null,
                type: dto.type,
            },
        });
    }

    listByUser(userId: string) {
        return this.prismaService.project.findMany({
            where: { userId },
            orderBy: { createdAt: 'desc' },
        });
    }
}
