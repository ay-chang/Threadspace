// src/projects/dto/create-project.dto.ts
import { IsEnum, IsOptional, IsString, MinLength } from 'class-validator';

export class CreateProjectDto {
    @IsString()
    @MinLength(2)
    name: string;

    @IsString()
    @IsOptional()
    description?: string;

    @IsEnum(['web', 'ios', 'android', 'backend', 'fullstack', 'other'] as const)
    type: 'web' | 'ios' | 'android' | 'backend' | 'fullstack' | 'other';

    // For now pass ownerId from FE; later take from auth
    @IsString()
    userId: string;
}
