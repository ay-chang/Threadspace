import {
    Injectable,
    UnauthorizedException,
    HttpException,
} from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';

type SummaryInput = { project: string; teamId?: string };

@Injectable()
export class VercelService {
    constructor(private readonly http: HttpService) {}

    private get authHeader() {
        const token = process.env.VERCEL_TOKEN;
        if (!token) throw new UnauthorizedException('VERCEL_TOKEN not set');
        return { Authorization: `Bearer ${token}` };
    }

    private withTeam(path: string, teamId?: string) {
        if (!teamId) return path;
        return (
            path +
            (path.includes('?') ? '&' : '?') +
            `teamId=${encodeURIComponent(teamId)}`
        );
    }

    async getSummary(input: SummaryInput) {
        const { project, teamId } = input;

        // 1) Project details
        const projectUrl = this.withTeam(
            `/v10/projects/${encodeURIComponent(project)}`,
            teamId,
        );
        const projRes = await this.safeGet(projectUrl);
        const proj = projRes;

        // 2) Latest deployment (limit 1)
        const deploymentsUrl = this.withTeam(
            `/v6/deployments?projectId=${encodeURIComponent(proj.id)}&limit=1`,
            teamId,
        );
        const depList = await this.safeGet(deploymentsUrl);
        const latest = depList?.deployments?.[0] ?? null;

        return {
            project: {
                id: proj.id,
                name: proj.name,
                framework: proj.framework ?? null,
                createdAt: proj.createdAt
                    ? new Date(proj.createdAt).toISOString()
                    : null,
            },
            latestDeployment: latest
                ? {
                      id: latest.id,
                      url: latest.url,
                      readyState: latest.readyState,
                      target: latest.target, // "production" | "preview"
                      createdAt: latest.createdAt
                          ? new Date(latest.createdAt).toISOString()
                          : null,
                  }
                : null,
        };
    }

    private async safeGet(path: string) {
        try {
            const { data } = await firstValueFrom(
                this.http.get(path, { headers: this.authHeader }),
            );
            return data;
        } catch (e: any) {
            // Surface upstream status cleanly
            const status = e?.response?.status ?? 500;
            const message =
                e?.response?.data ?? e?.message ?? 'Vercel API error';
            throw new HttpException(message, status);
        }
    }
}
