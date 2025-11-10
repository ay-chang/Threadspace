import { Body, Controller, Post } from '@nestjs/common';
import { ProjectsService } from './projects.service';
import { CreateProjectDto } from './dtos/create-project.dto';

@Controller('projects')
export class ProjectsController {
    constructor(private readonly projectService: ProjectsService) {}

    @Post()
    create(@Body() dto: CreateProjectDto) {
        return this.projectService.create(dto);
    }
}
