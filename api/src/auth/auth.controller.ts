import { Body, Controller, Post } from '@nestjs/common';
import { UsersService } from '../users/users.service';

@Controller('auth')
export class AuthController {
  constructor(private users: UsersService) {}

  @Post('google/upsert')
  async googleUpsert(
    @Body() body: { sub: string; email: string; name?: string },
  ) {
    const user = await this.users.upsertGoogleUser({
      providerId: body.sub,
      email: body.email,
      name: body.name,
    });
    return { id: user.id, email: user.email };
  }
}
