import {
    Card,
    CardAction,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";

export default function IntegrationCard() {
    return (
        <Card>
            <CardHeader>
                <CardTitle>Integretation</CardTitle>
                <CardDescription>Integretation Description</CardDescription>
                <CardAction>Add Integretation</CardAction>
            </CardHeader>
            <CardContent>
                <p>Integretation Content</p>
            </CardContent>
            <CardFooter>
                <p>Integretation Footer</p>
            </CardFooter>
        </Card>
    );
}
