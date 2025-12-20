import {
    NavigationMenu,
    NavigationMenuList,
    NavigationMenuItem,
    NavigationMenuLink,
    navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu";
import { authOptions } from "@/lib/authOptions";
import { getServerSession } from "next-auth";
import Link from "next/link";
import { Button } from "./ui/button";
import { User } from "lucide-react";

export default async function Navbar() {
    const session = await getServerSession(authOptions);
    const user = session?.user;
    const dashboardHref = user ? "/dashboard" : "/login";

    return (
        <header className="">
            <div className="mx-auto max-w-6xl px-4 h-14 flex items-center justify-between">
                {/* Left: brand + nav */}
                <div className="flex items-center gap-6">
                    <Link href="/" className="font-semibold">
                        Threadspace
                    </Link>
                    <NavigationMenu>
                        <NavigationMenuList>
                            {/** Features link */}
                            <NavigationMenuItem>
                                <NavigationMenuLink
                                    asChild
                                    className={navigationMenuTriggerStyle()}
                                >
                                    <Link href="/features">Features</Link>
                                </NavigationMenuLink>
                            </NavigationMenuItem>

                            {/** Docs link */}
                            <NavigationMenuItem>
                                <NavigationMenuLink
                                    asChild
                                    className={navigationMenuTriggerStyle()}
                                >
                                    <Link href="/docs">Docs</Link>
                                </NavigationMenuLink>
                            </NavigationMenuItem>

                            {/** Company link */}
                            <NavigationMenuItem>
                                <NavigationMenuLink
                                    asChild
                                    className={navigationMenuTriggerStyle()}
                                >
                                    <Link href="/company">Company</Link>
                                </NavigationMenuLink>
                            </NavigationMenuItem>
                        </NavigationMenuList>
                    </NavigationMenu>
                </div>

                {/* Right: user + auth */}
                <div className="flex items-center gap-6">
                    <Link href={dashboardHref}>
                        <p className="text-sm">Dashboard</p>
                    </Link>
                    {user ? (
                        <div className="flex items-center gap-3">
                            {user.image ? (
                                <img
                                    src={user.image}
                                    alt={`${user.name ?? "User"} profile`}
                                    className="h-9 w-9 rounded-full border border-border object-cover"
                                    referrerPolicy="no-referrer"
                                />
                            ) : (
                                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gray-200 text-gray-500">
                                    <User className="h-5 w-5" />
                                </div>
                            )}
                        </div>
                    ) : (
                        <Link href="/login">
                            <Button>Sign In</Button>
                        </Link>
                    )}
                </div>
            </div>
        </header>
    );
}
