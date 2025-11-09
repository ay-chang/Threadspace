import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
    Field,
    FieldDescription,
    FieldGroup,
    FieldLabel,
    FieldLegend,
    FieldSeparator,
    FieldSet,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

export default function CreateProjectPage() {
    return (
        <div className="flex w-full h-full items-center justify-center p-8">
            <div className="w-full max-w-md">
                <form>
                    <FieldGroup>
                        <FieldSet>
                            <FieldLegend>Create a new Project</FieldLegend>
                            <FieldDescription>
                                Create a new project and hook it up with integrations to a
                                bunch of different services.
                            </FieldDescription>
                            <Field>
                                <FieldLabel>Project Name</FieldLabel>
                                <Input id="" placeholder="Evil Rabbit" required />
                            </Field>
                            <Field>
                                <FieldLabel>Project Description</FieldLabel>
                                <Textarea id="" placeholder="1234 5678 9012 3456" required />
                                <FieldDescription>
                                    Give your project a description.
                                </FieldDescription>
                            </Field>
                        </FieldSet>

                        <FieldSeparator />

                        <FieldSet>
                            <FieldLegend>Project Settings</FieldLegend>
                            <FieldDescription>
                                Select a project type so that we can give recomendations of
                                which integrations you can hook up to your project.
                            </FieldDescription>
                            <Field>
                                <FieldLabel>Project Type</FieldLabel>
                                <Select defaultValue="">
                                    <SelectTrigger className="w-full" id="">
                                        <SelectValue placeholder="Web" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="web">Web</SelectItem>
                                        <SelectItem value="ios">IOS (Mobile)</SelectItem>
                                        <SelectItem value="backend">Backend/API</SelectItem>
                                        <SelectItem value="android">Android</SelectItem>
                                        <SelectItem value="full-stack">Full Stack</SelectItem>
                                        <SelectItem value="other">Other</SelectItem>
                                    </SelectContent>
                                </Select>
                            </Field>
                        </FieldSet>

                        <Field orientation="horizontal">
                            <Button type="submit">Submit</Button>
                            <Button variant="outline" type="button">
                                Cancel
                            </Button>
                        </Field>
                    </FieldGroup>
                </form>
            </div>
        </div>
    );
}
