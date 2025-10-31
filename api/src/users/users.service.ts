import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { User } from '@prisma/client';

type GoogleProfile = {
    providerId: string; // Google's "sub"
    email: string;
    name?: string | null;
};

@Injectable()
export class UsersService {
    constructor(private prisma: PrismaService) {}

    // Upsert: pdate if exists, otherwise insert
    async upsertGoogleUser(profile: GoogleProfile): Promise<User> {
        const { providerId, email, name } = profile;

        return this.prisma.user.upsert({
            where: { providerId },
            update: {
                email,
                name: name ?? undefined,
            },
            create: {
                provider: 'google',
                providerId,
                email,
                name: name ?? undefined,
            },
        });
    }

    async findByProviderId(providerId: string) {
        return this.prisma.user.findUnique({ where: { providerId } });
    }
}
