import { Controller, Get, Query, BadRequestException } from '@nestjs/common';
import { VercelService } from './vercel.service';

@Controller('api/integrations/vercel')
export class VercelController {
    constructor(private readonly vercel: VercelService) {}

    // GET /api/integrations/vercel/summary?project=<nameOrId>&teamId=<optional>
    @Get('summary')
    async summary(
        @Query('project') project?: string,
        @Query('teamId') teamId?: string,
    ) {
        if (!project) throw new BadRequestException('Missing project');
        return this.vercel.getSummary({ project, teamId });
    }
}
