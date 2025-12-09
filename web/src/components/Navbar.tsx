import {
    NavigationMenu,
    NavigationMenuList,
    NavigationMenuItem,
    NavigationMenuLink,
    navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu";
import Link from "next/link";
import { Button } from "./ui/button";

export default async function Navbar() {
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
                    <Link href="/dashboard">
                        <p className="text-sm">Dashboard</p>
                    </Link>
                    <Link href="/login">
                        <Button>Sign In</Button>
                    </Link>
                </div>
            </div>
        </header>
    );
}
