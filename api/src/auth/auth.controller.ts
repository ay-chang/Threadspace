import {
    Body,
    Controller,
    Headers,
    Post,
    UnauthorizedException,
} from '@nestjs/common';
import { UsersService } from '../users/users.service';

@Controller('auth')
export class AuthController {
    constructor(private usersService: UsersService) {}

    @Post('google/upsert')
    async googleUpsert(
        @Body() body: { providerId: string; email: string; name?: string },
        @Headers('x-internal-token') token?: string,
    ) {
        // Simple shared-secret check so only your Next server can call this
        if (token !== process.env.INTERNAL_SYNC_TOKEN) {
            throw new UnauthorizedException('Invalid sync token');
        }

        const user = await this.usersService.upsertGoogleUser({
            providerId: body.providerId,
            email: body.email,
            name: body.name,
        });
        return { id: user.id, email: user.email };
    }
}
